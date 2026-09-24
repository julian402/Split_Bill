package ue.edu.co.splitbill.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import ue.edu.co.splitbill.entity.ExpenseShare;
import ue.edu.co.splitbill.manager.DatabaseContract;

/**
 * Operaciones sobre las partes en que se reparte cada gasto.
 */
@Dao
public interface ExpenseShareDao {

    @Insert
    void insertAll(List<ExpenseShare> shares);

    @Query(DatabaseContract.ExpenseShares.SELECT_BY_EXPENSE)
    List<ExpenseShare> findByExpense(String expenseId);

    /** Para el detalle del gasto: trae el nombre de cada participante con un JOIN. */
    @Query(DatabaseContract.ExpenseShares.SELECT_BY_EXPENSE_WITH_NAMES)
    List<ShareListItem> findByExpenseWithNames(String expenseId);

    /** Aqui si se borra de verdad: una parte no tiene sentido sin su gasto. */
    @Query(DatabaseContract.ExpenseShares.DELETE_BY_EXPENSE)
    int deleteByExpense(String expenseId);
}
