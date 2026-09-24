package ue.edu.co.splitbill.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Upsert;

import java.util.List;

import ue.edu.co.splitbill.entity.Expense;
import ue.edu.co.splitbill.manager.DatabaseContract;

/**
 * Operaciones sobre la tabla de gastos.
 */
@Dao
public interface ExpenseDao {

    @Insert
    long insert(Expense expense);

    @Update
    int update(Expense expense);

    /** Lista para pintar la pantalla principal: trae el nombre del pagador con un JOIN. */
    @Query(DatabaseContract.Expenses.SELECT_ACTIVE_WITH_PAYER)
    List<ExpenseListItem> findActiveWithPayer(String groupId);

    @Query(DatabaseContract.Expenses.SELECT_BY_ID)
    Expense findById(String expenseId);

    /** Total gastado por el grupo, calculado con SUM en la base de datos y no recorriendo la lista. */
    @Query(DatabaseContract.Expenses.SELECT_TOTAL_CENTS)
    long sumActiveCents(String groupId);

    /** Borrado logico: el gasto se marca inactivo y deja de contar en los saldos. */
    @Query(DatabaseContract.Expenses.SOFT_DELETE)
    int softDelete(String expenseId);

    @Upsert
    void upsert(Expense expense);

    @Query(DatabaseContract.Expenses.SELECT_PENDING)
    List<Expense> findPending();

    @Query(DatabaseContract.Expenses.COUNT_PENDING)
    int countPending();

    @Query(DatabaseContract.Expenses.COUNT_ALL)
    int countAll();

    @Query(DatabaseContract.Expenses.MARK_SYNCED)
    void markSynced(String expenseId, int status);

    @Query(DatabaseContract.Expenses.SELECT_SYNCED_ACTIVE_IDS)
    List<String> findSyncedActiveIds(String groupId);

    @Query(DatabaseContract.Expenses.MARK_DELETED_BY_SERVER)
    void markDeletedByServer(String expenseId);
}
