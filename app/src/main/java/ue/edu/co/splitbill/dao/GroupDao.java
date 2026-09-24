package ue.edu.co.splitbill.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Upsert;

import java.util.List;

import ue.edu.co.splitbill.entity.Group;
import ue.edu.co.splitbill.manager.DatabaseContract;

/**
 * Operaciones sobre la tabla de grupos. Aparece en la entrega 3, cuando el grupo deja de ser siempre
 * el sembrado y pasa a ser el grupo del usuario en el servidor.
 */
@Dao
public interface GroupDao {

    @Insert
    void insert(Group group);

    @Upsert
    void upsert(Group group);

    @Query(DatabaseContract.Groups.SELECT_BY_ID)
    Group findById(String groupId);

    @Query(DatabaseContract.Groups.SELECT_PENDING)
    List<Group> findPending();

    @Query(DatabaseContract.Groups.COUNT_PENDING)
    int countPending();

    @Query(DatabaseContract.Groups.MARK_SYNCED)
    void markSynced(String groupId);

    @Query(DatabaseContract.Groups.COPY_WITH_NEW_ID)
    void copyWithNewId(String oldId, String newId, String ownerId);

    @Query(DatabaseContract.Groups.MOVE_EXPENSES)
    void moveExpenses(String oldId, String newId);

    @Query(DatabaseContract.Groups.DELETE_BY_ID)
    void deleteById(String groupId);
}
