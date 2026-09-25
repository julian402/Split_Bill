package ue.edu.co.splitbill.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import ue.edu.co.splitbill.dao.QuickSplitListItem;
import ue.edu.co.splitbill.di.AppExecutors;
import ue.edu.co.splitbill.domain.Share;
import ue.edu.co.splitbill.entity.QuickSplit;
import ue.edu.co.splitbill.entity.QuickSplitShare;
import ue.edu.co.splitbill.manager.SplitBillDatabase;
import ue.edu.co.splitbill.sync.SyncManager;

/**
 * Cuentas rapidas guardadas: la cuenta dividida al momento que la persona quiso conservar, sin
 * pasarla a ningun grupo. Se guardan en el celular y se suben al servidor por detras, como los gastos.
 */
public class QuickSplitRepository extends BaseRepository {

    private static final String TAG = "QuickSplitRepository";

    private final SyncManager syncManager;

    public QuickSplitRepository(SplitBillDatabase database, AppExecutors executors, SyncManager syncManager) {
        super(database, executors);
        this.syncManager = syncManager;
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    /**
     * Guarda la cuenta con lo que le toco a cada persona.
     *
     * @param names  el nombre de cada persona, en el mismo orden que shares
     * @param shares el reparto ya calculado por la SplitStrategy de la cuenta rapida
     */
    public void saveQuickSplit(final QuickSplit quickSplit, final List<String> names, final List<Share> shares,
                               DataCallback<QuickSplit> callback) {
        runAsync(new Callable<QuickSplit>() {
            @Override
            public QuickSplit call() {
                quickSplit.validar();
                if (shares.isEmpty() || names.size() != shares.size()) {
                    throw new IllegalArgumentException("Primero calcula cuánto le toca a cada uno");
                }
                final List<QuickSplitShare> rows = new ArrayList<>(shares.size());
                long total = 0;
                for (int i = 0; i < shares.size(); i++) {
                    rows.add(new QuickSplitShare(quickSplit.getId(), i, names.get(i), shares.get(i).getAmount()));
                    total += shares.get(i).getAmount().getCents();
                }
                if (total != quickSplit.getTotalCents()) {
                    throw new IllegalArgumentException("El reparto no suma el total de la cuenta");
                }
                database.runInTransaction(new Runnable() {
                    @Override
                    public void run() {
                        database.quickSplitDao().insert(quickSplit);
                        database.quickSplitDao().insertShares(rows);
                    }
                });
                syncManager.notifyLocalChange();
                return quickSplit;
            }
        }, callback);
    }

    public void getSavedQuickSplits(DataCallback<List<QuickSplitListItem>> callback) {
        runAsync(new Callable<List<QuickSplitListItem>>() {
            @Override
            public List<QuickSplitListItem> call() {
                return database.quickSplitDao().findActive();
            }
        }, callback);
    }

    /** Cuantas hay guardadas: se muestra en la tarjeta de la pestana Grupos. */
    public void countSavedQuickSplits(DataCallback<Integer> callback) {
        runAsync(new Callable<Integer>() {
            @Override
            public Integer call() {
                return database.quickSplitDao().countActive();
            }
        }, callback);
    }

    public void getQuickSplitDetail(final String quickSplitId, DataCallback<QuickSplitDetail> callback) {
        runAsync(new Callable<QuickSplitDetail>() {
            @Override
            public QuickSplitDetail call() {
                QuickSplit quickSplit = database.quickSplitDao().findById(quickSplitId);
                if (quickSplit == null || !quickSplit.isActive()) {
                    throw new IllegalStateException("La cuenta rápida ya no existe");
                }
                return new QuickSplitDetail(quickSplit, database.quickSplitDao().findShares(quickSplitId));
            }
        }, callback);
    }

    /** Borrado logico: deja el borrado en la cola para el servidor. */
    public void deleteQuickSplit(final String quickSplitId, DataCallback<Integer> callback) {
        runAsync(new Callable<Integer>() {
            @Override
            public Integer call() {
                int rows = database.quickSplitDao().softDelete(quickSplitId);
                syncManager.notifyLocalChange();
                return rows;
            }
        }, callback);
    }
}
