package ue.edu.co.splitbill.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Upsert;

import java.util.List;

import ue.edu.co.splitbill.entity.Message;
import ue.edu.co.splitbill.manager.DatabaseContract;

/** Mensajes del chat de cada grupo. */
@Dao
public interface MessageDao {

    @Insert
    void insert(Message message);

    @Upsert
    void upsert(Message message);

    @Query(DatabaseContract.Messages.SELECT_BY_ID)
    Message findById(String messageId);

    @Query(DatabaseContract.Messages.SELECT_BY_GROUP)
    List<Message> findByGroup(String groupId);

    @Query(DatabaseContract.Messages.SELECT_LAST)
    Message findLast(String groupId);

    @Query(DatabaseContract.Messages.SELECT_PENDING)
    List<Message> findPending();

    @Query(DatabaseContract.Messages.COUNT_PENDING)
    int countPending();

    @Query(DatabaseContract.Messages.MARK_SYNCED)
    void markSynced(String messageId);

    @Query(DatabaseContract.Messages.DELETE_BY_ID)
    void deleteById(String messageId);
}
