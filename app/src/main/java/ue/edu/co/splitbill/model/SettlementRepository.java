package ue.edu.co.splitbill.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import ue.edu.co.splitbill.dao.UserAmount;
import ue.edu.co.splitbill.di.AppExecutors;
import ue.edu.co.splitbill.domain.Balance;
import ue.edu.co.splitbill.domain.BalanceCalculator;
import ue.edu.co.splitbill.domain.DebtSimplifier;
import ue.edu.co.splitbill.domain.ExpenseCategory;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.domain.Share;
import ue.edu.co.splitbill.domain.SplitType;
import ue.edu.co.splitbill.domain.Transfer;
import ue.edu.co.splitbill.entity.Expense;
import ue.edu.co.splitbill.entity.ExpenseShare;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.manager.SplitBillDatabase;
import ue.edu.co.splitbill.sync.SyncManager;

/**
 * Repositorio de liquidacion: arma la respuesta de la pantalla "Liquidar".
 *
 * Encadena las tres piezas del proyecto:
 *   1. SQLite entrega, con SUM y GROUP BY, cuanto puso y cuanto debia poner cada integrante.
 *   2. BalanceCalculator convierte esos dos totales en el saldo neto de cada uno.
 *   3. DebtSimplifier convierte los saldos en el minimo numero de transferencias.
 *
 * Los pasos 2 y 3 son Java puro y estan probados con JUnit sin necesidad de emulador.
 *
 * Tambien registra los pagos ("Marcar como pagado"): cada transferencia hecha se guarda como un
 * gasto PAYMENT que paga el deudor y cuya unica parte es del acreedor. Asi, en la siguiente
 * liquidacion, BalanceCalculator deja a los dos en cero sin ninguna regla especial.
 */
public class SettlementRepository extends BaseRepository {

    private static final String TAG = "SettlementRepository";

    private final BalanceCalculator balanceCalculator;
    private final DebtSimplifier debtSimplifier;
    private final SyncManager syncManager;

    public SettlementRepository(SplitBillDatabase database, AppExecutors executors,
                                BalanceCalculator balanceCalculator, DebtSimplifier debtSimplifier,
                                SyncManager syncManager) {
        super(database, executors);
        this.balanceCalculator = balanceCalculator;
        this.debtSimplifier = debtSimplifier;
        this.syncManager = syncManager;
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    public void getSettlement(final String groupId, DataCallback<SettlementResult> callback) {
        runAsync(new Callable<SettlementResult>() {
            @Override
            public SettlementResult call() {
                List<User> users = database.groupMemberDao().findActiveUsers(groupId);

                Map<String, Money> totalPaid = aMapaDeMontos(database.balanceDao().sumPaidByUser(groupId));
                Map<String, Money> totalOwed = aMapaDeMontos(database.balanceDao().sumOwedByUser(groupId));

                List<String> userIds = new ArrayList<>(users.size());
                Map<String, String> userNames = new LinkedHashMap<>();
                for (User user : users) {
                    userIds.add(user.getId());
                    userNames.put(user.getId(), user.getNames());
                }

                List<Balance> balances = balanceCalculator.calcularBalances(userIds, totalPaid, totalOwed);
                List<Transfer> transfers = debtSimplifier.simplificar(balances);

                //Un integrante dado de baja puede seguir apareciendo en los saldos de gastos viejos
                for (Balance balance : balances) {
                    if (!userNames.containsKey(balance.getUserId())) {
                        User retired = database.userDao().findById(balance.getUserId());
                        if (retired != null) {
                            userNames.put(retired.getId(), retired.getNames());
                        }
                    }
                }
                return new SettlementResult(balances, transfers, userNames,
                        database.balanceDao().countDirectTransfers(groupId));
            }
        }, callback);
    }

    /**
     * Registra como pagadas las transferencias indicadas, todas en una sola transaccion.
     *
     * @return cuantos pagos se registraron
     */
    public void markPaid(final String groupId, final List<Transfer> transfers, DataCallback<Integer> callback) {
        runAsync(new Callable<Integer>() {
            @Override
            public Integer call() {
                final List<Expense> payments = new ArrayList<>(transfers.size());
                final List<ExpenseShare> shares = new ArrayList<>(transfers.size());
                for (Transfer transfer : transfers) {
                    Expense payment = toPayment(groupId, transfer);
                    payment.validar();
                    payments.add(payment);
                    shares.add(new ExpenseShare(payment.getId(),
                            new Share(transfer.getToUserId(), transfer.getAmount())));
                }
                database.runInTransaction(new Runnable() {
                    @Override
                    public void run() {
                        for (Expense payment : payments) {
                            database.expenseDao().insert(payment);
                        }
                        database.expenseShareDao().insertAll(shares);
                    }
                });
                syncManager.notifyLocalChange();
                return payments.size();
            }
        }, callback);
    }

    /** "Luis le paga a Andres": gasto que paga Luis y cuya unica parte (el monto completo) es de Andres. */
    private Expense toPayment(String groupId, Transfer transfer) {
        User payee = database.userDao().findById(transfer.getToUserId());
        String description = "Pago a " + (payee == null ? "integrante" : payee.getNames());
        Expense payment = new Expense(groupId, transfer.getFromUserId(), description, transfer.getAmount(),
                SplitType.EXACT);
        payment.setCategory(ExpenseCategory.PAYMENT);
        return payment;
    }

    private Map<String, Money> aMapaDeMontos(List<UserAmount> amounts) {
        Map<String, Money> result = new LinkedHashMap<>();
        for (UserAmount amount : amounts) {
            result.put(amount.getUserId(), amount.getTotal());
        }
        return result;
    }
}
