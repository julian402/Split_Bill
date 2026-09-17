package ue.edu.co.splitbill.dao;

import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

import ue.edu.co.splitbill.manager.DatabaseContract;

/**
 * Consultas de agregacion que alimentan el calculo de saldos.
 *
 * Son las dos mitades de la formula "saldo = lo que pago - lo que le correspondia pagar". El trabajo
 * pesado lo hace SQLite con SUM y GROUP BY: en vez de traer todos los gastos y todas sus partes a
 * memoria para recorrerlos en Java, la base de datos devuelve un solo total por integrante.
 */
@Dao
public interface BalanceDao {

    /** Cuanto puso cada integrante: agrupa los gastos activos por quien los pago. */
    @Query(DatabaseContract.Expenses.SELECT_TOTAL_PAID_BY_USER)
    List<UserAmount> sumPaidByUser(String groupId);

    /** Cuanto le correspondia pagar a cada integrante: une las partes con su gasto y las agrupa. */
    @Query(DatabaseContract.ExpenseShares.SELECT_TOTAL_OWED_BY_USER)
    List<UserAmount> sumOwedByUser(String groupId);

    /** Cuantas transferencias harian falta si cada quien le devolviera su parte a quien pago. */
    @Query(DatabaseContract.ExpenseShares.COUNT_DIRECT_TRANSFERS)
    int countDirectTransfers(String groupId);
}
