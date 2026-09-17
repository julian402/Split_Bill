package ue.edu.co.splitbill.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.manager.DatabaseContract;

/**
 * Operaciones sobre la tabla de integrantes.
 *
 * Las consultas no se escriben aqui sino en DatabaseContract, que es la unica fuente de verdad del
 * esquema. Room las verifica contra las tablas reales en tiempo de compilacion.
 */
@Dao
public interface UserDao {

    @Insert
    long insert(User user);

    @Update
    int update(User user);

    @Query(DatabaseContract.Users.SELECT_ACTIVE)
    List<User> findActive();

    @Query(DatabaseContract.Users.SELECT_BY_ID)
    User findById(String userId);

    @Query(DatabaseContract.Users.SELECT_COUNT_ACTIVE)
    int countActive();

    /** Borrado logico: la fila se marca inactiva para no romper los gastos que la referencian. */
    @Query(DatabaseContract.Users.SOFT_DELETE)
    int softDelete(String userId);
}
