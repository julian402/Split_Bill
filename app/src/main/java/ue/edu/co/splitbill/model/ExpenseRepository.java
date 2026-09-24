package ue.edu.co.splitbill.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import ue.edu.co.splitbill.dao.ExpenseListItem;
import ue.edu.co.splitbill.di.AppExecutors;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.domain.Share;
import ue.edu.co.splitbill.domain.split.SplitRequest;
import ue.edu.co.splitbill.domain.split.SplitStrategyFactory;
import ue.edu.co.splitbill.entity.Expense;
import ue.edu.co.splitbill.entity.ExpenseShare;
import ue.edu.co.splitbill.entity.SyncStatus;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.manager.SplitBillDatabase;
import ue.edu.co.splitbill.sync.SyncManager;

/**
 * Repositorio de gastos.
 *
 * Aqui se une el dominio con la persistencia: el repositorio le pide a la fabrica la estrategia de
 * division que corresponda, la aplica para obtener las partes y guarda el gasto junto con sus partes
 * dentro de una misma transaccion.
 */
public class ExpenseRepository extends BaseRepository {

    private static final String TAG = "ExpenseRepository";

    private final SyncManager syncManager;

    /** @param syncManager se le avisa despues de cada cambio para que lo suba al servidor cuando pueda */
    public ExpenseRepository(SplitBillDatabase database, AppExecutors executors, SyncManager syncManager) {
        super(database, executors);
        this.syncManager = syncManager;
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    /**
     * Registra un gasto y lo reparte entre sus participantes.
     *
     * El gasto y sus partes se guardan en una sola transaccion: o quedan los dos, o no queda ninguno.
     * Si se guardara primero el gasto y fallara el reparto, quedaria un gasto sin partes y los saldos
     * del grupo no cuadrarian.
     *
     * @param expense datos del gasto ya armados por la pantalla
     * @param request participantes y valores digitados segun el tipo de division
     */
    public void insertExpense(final Expense expense, final SplitRequest request,
                              DataCallback<Expense> callback) {
        runAsync(new Callable<Expense>() {
            @Override
            public Expense call() {
                expense.validar();

                //La pantalla no conoce las clases concretas de division: se las pide a la fabrica
                List<Share> shares = SplitStrategyFactory.create(expense.getSplitType()).split(request);

                final List<ExpenseShare> expenseShares = new ArrayList<>(shares.size());
                for (Share share : shares) {
                    expenseShares.add(new ExpenseShare(expense.getId(), share));
                }

                Expense saved = database.runInTransaction(new Callable<Expense>() {
                    @Override
                    public Expense call() {
                        database.expenseDao().insert(expense);
                        database.expenseShareDao().insertAll(expenseShares);
                        return expense;
                    }
                });
                //el gasto ya esta guardado en el celular; subirlo al servidor ocurre por detras
                syncManager.requestSync();
                return saved;
            }
        }, callback);
    }

    /**
     * Guarda los cambios de un gasto que ya existe y vuelve a repartirlo.
     *
     * Igual que al crear, el gasto y sus partes se guardan en una sola transaccion. Las partes viejas
     * se borran y se escriben las nuevas: es mas simple y seguro que comparar una por una. El gasto
     * queda como PENDING_UPDATE para que el SyncManager suba el cambio; si todavia no se habia subido
     * (PENDING_CREATE), sigue asi y se sube de una vez con los datos nuevos.
     */
    public void updateExpense(final Expense expense, final SplitRequest request,
                              DataCallback<Expense> callback) {
        runAsync(new Callable<Expense>() {
            @Override
            public Expense call() {
                expense.validar();
                final Expense current = database.expenseDao().findById(expense.getId());
                if (current == null || !current.isActive()) {
                    throw new IllegalArgumentException("Este gasto ya no existe");
                }

                List<Share> shares = SplitStrategyFactory.create(expense.getSplitType()).split(request);
                final List<ExpenseShare> expenseShares = new ArrayList<>(shares.size());
                for (Share share : shares) {
                    expenseShares.add(new ExpenseShare(expense.getId(), share));
                }

                //la fecha y el grupo no se editan; el estado de sincronizacion depende de si ya se subio
                expense.setDate(current.getDate());
                expense.setGroupId(current.getGroupId());
                expense.setSyncStatus(current.getSyncStatus() == SyncStatus.PENDING_CREATE
                        ? SyncStatus.PENDING_CREATE : SyncStatus.PENDING_UPDATE);

                Expense saved = database.runInTransaction(new Callable<Expense>() {
                    @Override
                    public Expense call() {
                        database.expenseDao().update(expense);
                        database.expenseShareDao().deleteByExpense(expense.getId());
                        database.expenseShareDao().insertAll(expenseShares);
                        return expense;
                    }
                });
                syncManager.requestSync();
                return saved;
            }
        }, callback);
    }

    /** Gastos del grupo con el nombre de quien pago, listos para pintar el RecyclerView. */
    public void getActiveExpenses(final String groupId, DataCallback<List<ExpenseListItem>> callback) {
        runAsync(new Callable<List<ExpenseListItem>>() {
            @Override
            public List<ExpenseListItem> call() {
                return database.expenseDao().findActiveWithPayer(groupId);
            }
        }, callback);
    }

    /** Total gastado por el grupo. Lo suma SQLite, no la aplicacion. */
    public void getTotalExpenses(final String groupId, DataCallback<Money> callback) {
        runAsync(new Callable<Money>() {
            @Override
            public Money call() {
                return Money.ofCents(database.expenseDao().sumActiveCents(groupId));
            }
        }, callback);
    }

    public void searchExpenseById(final String expenseId, DataCallback<Expense> callback) {
        runAsync(new Callable<Expense>() {
            @Override
            public Expense call() {
                Expense expense = database.expenseDao().findById(expenseId);
                if (expense == null) {
                    throw new IllegalArgumentException("No se encontró el gasto");
                }
                return expense;
            }
        }, callback);
    }

    /**
     * Detalle de un gasto: el gasto, el nombre de quien pago y cada parte con el nombre del participante.
     * Si el gasto ya no existe (por ejemplo, otro integrante lo borro y llego con la sincronizacion),
     * se avisa con un mensaje para el usuario.
     */
    public void getExpenseDetail(final String expenseId, DataCallback<ExpenseDetail> callback) {
        runAsync(new Callable<ExpenseDetail>() {
            @Override
            public ExpenseDetail call() {
                Expense expense = database.expenseDao().findById(expenseId);
                if (expense == null || !expense.isActive()) {
                    throw new IllegalArgumentException("Este gasto ya no existe");
                }
                User payer = database.userDao().findById(expense.getPayerId());
                String payerNames = payer == null ? "" : payer.getNames();
                return new ExpenseDetail(expense, payerNames,
                        database.expenseShareDao().findByExpenseWithNames(expenseId));
            }
        }, callback);
    }

    /** Borrado logico: el gasto deja de aparecer y de contar en los saldos. */
    public void deleteExpense(final String expenseId, DataCallback<Integer> callback) {
        runAsync(new Callable<Integer>() {
            @Override
            public Integer call() {
                int rowsAffected = database.expenseDao().softDelete(expenseId);
                if (rowsAffected == 0) {
                    throw new IllegalArgumentException("No se encontró el gasto");
                }
                syncManager.requestSync();
                return rowsAffected;
            }
        }, callback);
    }
}
