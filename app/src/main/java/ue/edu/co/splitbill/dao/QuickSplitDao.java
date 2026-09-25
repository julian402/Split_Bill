package ue.edu.co.splitbill.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Upsert;

import java.util.List;

import ue.edu.co.splitbill.entity.QuickSplit;
import ue.edu.co.splitbill.entity.QuickSplitShare;
import ue.edu.co.splitbill.manager.DatabaseContract;

/** Cuentas rapidas guardadas y sus partes. Una cuenta y sus partes siempre se escriben juntas. */
@Dao
public interface QuickSplitDao {

    @Insert
    void insert(QuickSplit quickSplit);

    @Upsert
    void upsert(QuickSplit quickSplit);

    @Insert
    void insertShares(List<QuickSplitShare> shares);

    @Query(DatabaseContract.QuickSplits.SELECT_ACTIVE)
    List<QuickSplitListItem> findActive();

    @Query(DatabaseContract.QuickSplits.SELECT_BY_ID)
    QuickSplit findById(String quickSplitId);

    @Query(DatabaseContract.QuickSplitShares.SELECT_BY_QUICK_SPLIT)
    List<QuickSplitShare> findShares(String quickSplitId);

    @Query(DatabaseContract.QuickSplitShares.DELETE_BY_QUICK_SPLIT)
    void deleteShares(String quickSplitId);

    @Query(DatabaseContract.QuickSplits.SOFT_DELETE)
    int softDelete(String quickSplitId);

    @Query(DatabaseContract.QuickSplits.SELECT_PENDING)
    List<QuickSplit> findPending();

    @Query(DatabaseContract.QuickSplits.COUNT_PENDING)
    int countPending();

    @Query(DatabaseContract.QuickSplits.MARK_SYNCED)
    void markSynced(String quickSplitId, int status);

    @Query(DatabaseContract.QuickSplits.SELECT_SYNCED_ACTIVE_IDS)
    List<String> findSyncedActiveIds();

    @Query(DatabaseContract.QuickSplits.MARK_DELETED_BY_SERVER)
    void markDeletedByServer(String quickSplitId);
}
