package ue.edu.co.splitbill.ui.home;

import android.content.Intent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.dao.ActivityItem;
import ue.edu.co.splitbill.dao.ExpenseListItem;
import ue.edu.co.splitbill.dao.GroupListItem;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.model.DashboardRepository;
import ue.edu.co.splitbill.model.DashboardSummary;
import ue.edu.co.splitbill.model.GroupRepository;
import ue.edu.co.splitbill.sync.NetworkMonitor;
import ue.edu.co.splitbill.sync.SyncListener;
import ue.edu.co.splitbill.sync.SyncManager;
import ue.edu.co.splitbill.sync.SyncResult;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.SyncStatusText;
import ue.edu.co.splitbill.ui.adapter.ExpenseAdapter;
import ue.edu.co.splitbill.ui.adapter.GroupAdapter;
import ue.edu.co.splitbill.ui.expense.ExpenseDetailActivity;
import ue.edu.co.splitbill.ui.feed.ActivityFeedActivity;
import ue.edu.co.splitbill.ui.group.GroupDetailActivity;
import ue.edu.co.splitbill.ui.group.GroupFormActivity;
import ue.edu.co.splitbill.ui.group.GroupsActivity;
import ue.edu.co.splitbill.ui.quick.QuickSplitActivity;

/**
 * Inicio: el resumen de TODOS los grupos de la persona. Es la primera pantalla despues del login.
 *
 * Muestra cuanto se ha gastado, cuanto este mes, cuanto le toco y su saldo; sus grupos en un
 * carrusel (tocar uno lo abre) y la actividad reciente. Todo sale de la base de datos del celular,
 * asi que funciona igual sin conexion; al terminar cada sincronizacion se recarga.
 */
public class HomeActivity extends BaseActivity
        implements GroupAdapter.OnGroupListener, ExpenseAdapter.OnExpenseClickListener, SyncListener {

    private TextView tvGreeting;
    private TextView tvSyncStatus;
    private ImageButton btnNotifications;
    private View vPendingDot;
    private TextView segGroups;
    private TextView segQuickSplit;
    private TextView tvHomeTotal;
    private TextView tvHomeCounts;
    private TextView tvMonthTotal;
    private TextView tvMyShare;
    private TextView tvMyBalance;
    private View btnCreateGroup;
    private View btnQuickSplit;
    private TextView tvSeeAllGroups;
    private TextView tvSeeAllActivity;
    private TextView tvEmptyActivity;
    private RecyclerView rvGroups;
    private RecyclerView rvRecent;

    private GroupAdapter groupAdapter;
    private ExpenseAdapter expenseAdapter;
    private DashboardRepository dashboardRepository;
    private GroupRepository groupRepository;
    private SyncManager syncManager;
    private NetworkMonitor networkMonitor;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_home;
    }

    @Override
    protected int getNavItem() {
        return R.id.navHome;
    }

    @Override
    protected void initListeners() {
        this.btnNotifications.setOnClickListener(this::openActivity);
        this.segGroups.setOnClickListener(this::openGroups);
        this.segQuickSplit.setOnClickListener(this::openQuickSplit);
        this.btnCreateGroup.setOnClickListener(this::openCreateGroup);
        this.btnQuickSplit.setOnClickListener(this::openQuickSplit);
        this.tvSeeAllGroups.setOnClickListener(this::openGroups);
        this.tvSeeAllActivity.setOnClickListener(this::openActivity);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //el nombre pudo cambiar en el perfil
        this.tvGreeting.setText(getString(R.string.tvGreetingUser,
                getServiceLocator().getSessionManager().getUserNames()));
        loadDashboardDB();
        //y se traen los cambios que otros integrantes hayan hecho en el servidor
        this.syncManager.addListener(this);
        this.syncManager.requestSync();
        suggestClaimOnce();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.syncManager.removeListener(this);
    }

    /**
     * Despues de subir a la cuenta los datos que habia en el celular, se pregunta una sola vez si la
     * persona estaba en la lista de integrantes, para que no quede duplicada.
     */
    private void suggestClaimOnce() {
        if (!getServiceLocator().getSessionManager().shouldSuggestClaim()) {
            return;
        }
        getServiceLocator().getSessionManager().setSuggestClaim(false);
        confirm(getString(R.string.dlgSuggestClaimTitle), getString(R.string.dlgSuggestClaimMessage),
                R.string.btnSeeMembers, R.string.btnNotNow,
                () -> startActivity(GroupFormActivity.editIntent(this)));
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
        for (String rejected : result.getRejectedMessages()) {
            showToast(getString(R.string.msgChangeRejected, rejected));
        }
        this.tvSyncStatus.setText(SyncStatusText.of(this, result));
        this.vPendingDot.setVisibility(result.getPendingChanges() > 0 ? View.VISIBLE : View.GONE);
        loadDashboardDB();
    }

    private void loadDashboardDB() {
        this.dashboardRepository.getDashboard(new UiCallback<DashboardSummary>() {
            @Override
            protected void onData(DashboardSummary data) {
                showDashboard(data);
            }
        });
    }

    private void showDashboard(DashboardSummary summary) {
        this.tvHomeTotal.setText(summary.getTotal().format());
        this.tvHomeCounts.setText(getString(R.string.tvHomeCounts,
                getResources().getQuantityString(R.plurals.tvGroupCount, summary.getGroupCount(), summary.getGroupCount()),
                getResources().getQuantityString(R.plurals.tvExpenseCount, summary.getExpenseCount(), summary.getExpenseCount())));
        this.tvMonthTotal.setText(summary.getMonthTotal().format());
        this.tvMyShare.setText(summary.getMyShare().format());
        showBalance(summary.getMyBalance());

        this.groupAdapter.setGroups(summary.getGroups(), this.groupRepository.getCurrentGroupId());
        this.expenseAdapter.setExpenses(summary.getRecent());
        this.tvEmptyActivity.setVisibility(summary.getRecent().isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Verde si le deben, rojo claro si debe: sobre el degradado se usan tonos claros. */
    private void showBalance(Money balance) {
        this.tvMyBalance.setText(balance.format());
        int color = balance.isPositive() ? R.color.colorOnHeroPositive
                : balance.isNegative() ? R.color.colorOnHeroNegative : R.color.colorOnHero;
        this.tvMyBalance.setTextColor(ContextCompat.getColor(this, color));
    }

    /** Tocar un grupo lo vuelve el grupo actual y lo abre. */
    @Override
    public void onGroupClick(GroupListItem group) {
        this.groupRepository.switchGroup(group.getGroupId());
        startActivity(new Intent(this, GroupDetailActivity.class));
    }

    /** El menu de la tarjeta abre el grupo para editarlo: nombre e integrantes. */
    @Override
    public void onGroupMenu(GroupListItem group, View anchor) {
        this.groupRepository.switchGroup(group.getGroupId());
        startActivity(GroupFormActivity.editIntent(this));
    }

    /**
     * El gasto puede ser de otro grupo: se entra a ese grupo antes de abrirlo, para que editarlo use
     * sus integrantes.
     */
    @Override
    public void onExpenseClick(ExpenseListItem expense) {
        if (expense instanceof ActivityItem) {
            String groupId = ((ActivityItem) expense).getGroupId();
            if (!groupId.equals(this.groupRepository.getCurrentGroupId())) {
                this.groupRepository.switchGroup(groupId);
            }
        }
        Intent intent = new Intent(this, ExpenseDetailActivity.class);
        intent.putExtra(ExpenseDetailActivity.EXTRA_EXPENSE_ID, expense.getExpenseId());
        startActivity(intent);
    }

    private void openGroups(View view) {
        openTab(GroupsActivity.class);
    }

    private void openActivity(View view) {
        openTab(ActivityFeedActivity.class);
    }

    /** La cuenta rapida no necesita integrantes registrados: se abre siempre. */
    private void openQuickSplit(View view) {
        startActivity(new Intent(this, QuickSplitActivity.class));
    }

    private void openCreateGroup(View view) {
        startActivity(new Intent(this, GroupFormActivity.class));
    }

    @Override
    protected void initObjects() {
        this.tvGreeting = findViewById(R.id.tvGreeting);
        this.tvSyncStatus = findViewById(R.id.tvSyncStatus);
        this.btnNotifications = findViewById(R.id.btnNotifications);
        this.vPendingDot = findViewById(R.id.vPendingDot);
        this.segGroups = findViewById(R.id.segGroups);
        this.segQuickSplit = findViewById(R.id.segQuickSplit);
        this.tvHomeTotal = findViewById(R.id.tvHomeTotal);
        this.tvHomeCounts = findViewById(R.id.tvHomeCounts);
        this.tvMonthTotal = findViewById(R.id.tvMonthTotal);
        this.tvMyShare = findViewById(R.id.tvMyShare);
        this.tvMyBalance = findViewById(R.id.tvMyBalance);
        this.btnCreateGroup = findViewById(R.id.btnCreateGroup);
        this.btnQuickSplit = findViewById(R.id.btnQuickSplit);
        this.tvSeeAllGroups = findViewById(R.id.tvSeeAllGroups);
        this.tvSeeAllActivity = findViewById(R.id.tvSeeAllActivity);
        this.tvEmptyActivity = findViewById(R.id.tvEmptyActivity);
        this.rvGroups = findViewById(R.id.rvGroups);
        this.rvRecent = findViewById(R.id.rvRecent);

        this.dashboardRepository = getServiceLocator().getDashboardRepository();
        this.groupRepository = getServiceLocator().getGroupRepository();
        this.syncManager = getServiceLocator().getSyncManager();
        this.networkMonitor = getServiceLocator().getNetworkMonitor();

        this.groupAdapter = new GroupAdapter(this, true);
        this.rvGroups.setAdapter(this.groupAdapter);
        this.expenseAdapter = new ExpenseAdapter(getResources().getStringArray(R.array.splitTypes), this, false);
        this.rvRecent.setAdapter(this.expenseAdapter);

        this.tvHomeTotal.setText(Money.ZERO.format());
        this.tvSyncStatus.setText(this.networkMonitor.isOnline() ? R.string.tvSyncing : R.string.tvOffline);
    }
}
