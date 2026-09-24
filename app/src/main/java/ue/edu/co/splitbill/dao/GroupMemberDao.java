package ue.edu.co.splitbill.dao;

import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Upsert;

import java.util.List;

import ue.edu.co.splitbill.entity.GroupMember;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.manager.DatabaseContract;

/**
 * Quien esta en cada grupo. Desde la version 3 del esquema las listas de integrantes se leen aqui,
 * cruzando con users para tener el nombre y el telefono.
 */
@Dao
public interface GroupMemberDao {

    @Upsert
    void upsert(GroupMember member);

    @Query(DatabaseContract.GroupMembers.SELECT_BY_ID)
    GroupMember findById(String groupId, String userId);

    @Query(DatabaseContract.GroupMembers.SELECT_ACTIVE_USERS)
    List<User> findActiveUsers(String groupId);

    @Query(DatabaseContract.GroupMembers.COUNT_ACTIVE)
    int countActive(String groupId);

    /** Retira a la persona del grupo (borrado logico) y deja el cambio pendiente de subir. */
    @Query(DatabaseContract.GroupMembers.SOFT_DELETE)
    int softDelete(String groupId, String userId);

    @Query(DatabaseContract.GroupMembers.SELECT_PENDING)
    List<GroupMember> findPending();

    @Query(DatabaseContract.GroupMembers.COUNT_PENDING)
    int countPending();

    @Query(DatabaseContract.GroupMembers.MARK_SYNCED)
    void markSynced(String groupId, String userId, int status);
}
