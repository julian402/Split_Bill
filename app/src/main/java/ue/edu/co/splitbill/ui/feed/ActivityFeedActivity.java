package ue.edu.co.splitbill.ui.feed;

import android.content.Intent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.dao.ActivityItem;
import ue.edu.co.splitbill.dao.ExpenseListItem;
import ue.edu.co.splitbill.model.DashboardRepository;
import ue.edu.co.splitbill.model.GroupRepository;
import ue.edu.co.splitbill.sync.SyncListener;
import ue.edu.co.splitbill.sync.SyncManager;
import ue.edu.co.splitbill.sync.SyncResult;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.SyncStatusText;
import ue.edu.co.splitbill.ui.adapter.ExpenseAdapter;
import ue.edu.co.splitbill.ui.expense.ExpenseDetailActivity;

/**
 * Pestana Actividad: todos los gastos y pagos de todos los grupos, del mas reciente al mas antiguo,
 * agrupados por dia. Arriba va el estado de la sincronizacion y el boton para sincronizar a mano.
 */
public class ActivityFeedActivity extends BaseActivity
        implements ExpenseAdapter.OnExpenseClickListener, SyncListener {

    private TextView tvSyncStatus;
    private ImageButton btnSync;
    private TextView tvEmptyActivity;
    private RecyclerView rvActivity;

    private ExpenseAdapter expenseAdapter;
    private DashboardRepository dashboardRepository;
    private GroupRepository groupRepository;
    private SyncManager syncManager;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_feed;
    }

    @Override
    protected int getNavItem() {
        return R.id.navActivity;
    }

    @Override
    protected void initListeners() {
        this.btnSync.setOnClickListener(this::syncAPI);
    }

    @Override
    protected void onResume() {
        super.onResume();
        listActivityDB();
        this.syncManager.addListener(this);
        this.syncManager.requestSync();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.syncManager.removeListener(this);
    }

    private void syncAPI(View view) {
        this.syncManager.requestSync();
    }

    @Override
    public void onSyncStarted() {
        this.tvSyncStatus.setText(R.string.tvSyncing);
    }

    @Override
    public void onSyncFinished(SyncResult result) {
        if (!isAlive()) {
            return;
        }
        if (result.getState() == SyncResult.State.SESSION_EXPIRED) {
            goToLogin(true);
            return;
        }
        this.tvSyncStatus.setText(SyncStatusText.of(this, result));
        listActivityDB();
    }

    private void listActivityDB() {
        this.dashboardRepository.getActivity(new UiCallback<List<ActivityItem>>() {
            @Override
            protected void onData(List<ActivityItem> data) {
                expenseAdapter.setExpenses(data);
                tvEmptyActivity.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    /** El gasto puede ser de otro grupo: se entra a ese grupo antes de abrirlo. */
    @Override
    public void onExpenseClick(ExpenseListItem expense) {
        String groupId = ((ActivityItem) expense).getGroupId();
        if (!groupId.equals(this.groupRepository.getCurrentGroupId())) {
            this.groupRepository.switchGroup(groupId);
        }
        Intent intent = new Intent(this, ExpenseDetailActivity.class);
        intent.putExtra(ExpenseDetailActivity.EXTRA_EXPENSE_ID, expense.getExpenseId());
        startActivity(intent);
    }

    @Override
    protected void initObjects() {
        this.tvSyncStatus = findViewById(R.id.tvSyncStatus);
        this.btnSync = findViewById(R.id.btnSync);
        this.tvEmptyActivity = findViewById(R.id.tvEmptyActivity);
        this.rvActivity = findViewById(R.id.rvActivity);

        this.dashboardRepository = getServiceLocator().getDashboardRepository();
        this.groupRepository = getServiceLocator().getGroupRepository();
        this.syncManager = getServiceLocator().getSyncManager();

        this.expenseAdapter = new ExpenseAdapter(getResources().getStringArray(R.array.splitTypes), this, true);
        this.rvActivity.setAdapter(this.expenseAdapter);
        this.tvSyncStatus.setText(getServiceLocator().getNetworkMonitor().isOnline()
                ? R.string.tvSyncing : R.string.tvOffline);
    }
}
