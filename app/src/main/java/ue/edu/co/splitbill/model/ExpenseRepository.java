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
import ue.edu.co.splitbill.manager.SplitBillDatabase;

/**
 * Repositorio de gastos.
 *
 * Aqui se une el dominio con la persistencia: el repositorio le pide a la fabrica la estrategia de
 * division que corresponda, la aplica para obtener las partes y guarda el gasto junto con sus partes
 * dentro de una misma transaccion.
 */
public class ExpenseRepository extends BaseRepository {

    private static final String TAG = "ExpenseRepository";

    public ExpenseRepository(SplitBillDatabase database, AppExecutors executors) {
        super(database, executors);
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

                return database.runInTransaction(new Callable<Expense>() {
                    @Override
                    public Expense call() {
                        database.expenseDao().insert(expense);
                        database.expenseShareDao().insertAll(expenseShares);
                        return expense;
                    }
                });
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
                    throw new IllegalArgumentException("No se encontro el gasto");
                }
                return expense;
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
                    throw new IllegalArgumentException("No se encontro el gasto");
                }
                return rowsAffected;
            }
        }, callback);
    }
}
