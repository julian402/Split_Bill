package ue.edu.co.splitbill.model;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Callable;

import ue.edu.co.splitbill.dao.ActivityItem;
import ue.edu.co.splitbill.dao.GroupListItem;
import ue.edu.co.splitbill.di.AppExecutors;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.manager.SplitBillDatabase;
import ue.edu.co.splitbill.session.SessionManager;

/**
 * Repositorio de la pantalla de inicio y de la actividad: junta datos de TODOS los grupos.
 *
 * Todo sale de la base de datos del celular (el SyncManager trae los cambios de cada grupo), asi que
 * el inicio funciona igual sin conexion.
 */
public class DashboardRepository extends BaseRepository {

    private static final String TAG = "DashboardRepository";

    /** Cuantos gastos se muestran en "Actividad reciente" del inicio. */
    public static final int RECENT_LIMIT = 5;
    /** Tope de la pantalla de actividad: suficiente para meses de uso sin cargar la lista de mas. */
    public static final int ACTIVITY_LIMIT = 200;

    private final SessionManager sessionManager;

    public DashboardRepository(SplitBillDatabase database, AppExecutors executors, SessionManager sessionManager) {
        super(database, executors);
        this.sessionManager = sessionManager;
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    public void getDashboard(DataCallback<DashboardSummary> callback) {
        runAsync(new Callable<DashboardSummary>() {
            @Override
            public DashboardSummary call() {
                String userId = sessionManager.getUserId();
                List<GroupListItem> groups = database.groupDao().findActiveWithTotals(userId);

                long balance = 0;
                int expenseCount = 0;
                for (GroupListItem group : groups) {
                    balance += group.getBalanceCents();
                    expenseCount += group.getExpenseCount();
                }
                GroupRepository.fillMemberNames(database, groups);

                return new DashboardSummary(
                        Money.ofCents(database.expenseDao().sumAllGroupsSince(0L)),
                        Money.ofCents(database.expenseDao().sumAllGroupsSince(startOfMonth())),
                        Money.ofCents(database.expenseDao().sumUserSharesAllGroups(userId)),
                        Money.ofCents(balance),
                        expenseCount,
                        groups,
                        database.expenseDao().findRecentAllGroups(RECENT_LIMIT));
            }
        }, callback);
    }

    /** Toda la actividad (gastos y pagos) de todos los grupos, la mas reciente primero. */
    public void getActivity(DataCallback<List<ActivityItem>> callback) {
        runAsync(new Callable<List<ActivityItem>>() {
            @Override
            public List<ActivityItem> call() {
                return database.expenseDao().findRecentAllGroups(ACTIVITY_LIMIT);
            }
        }, callback);
    }

    /** Medianoche del dia 1 del mes actual, en la hora del celular. */
    static long startOfMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
