package ue.edu.co.splitbill.ui.group;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.dao.ExpenseListItem;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.entity.Group;
import ue.edu.co.splitbill.model.ExpenseRepository;
import ue.edu.co.splitbill.model.GroupRepository;
import ue.edu.co.splitbill.model.SessionRepository;
import ue.edu.co.splitbill.model.UserRepository;
import ue.edu.co.splitbill.sync.NetworkMonitor;
import ue.edu.co.splitbill.sync.SyncListener;
import ue.edu.co.splitbill.sync.SyncManager;
import ue.edu.co.splitbill.sync.SyncResult;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.adapter.ExpenseAdapter;
import ue.edu.co.splitbill.ui.expense.AddExpenseActivity;
import ue.edu.co.splitbill.ui.expense.ExpenseDetailActivity;
import ue.edu.co.splitbill.ui.quick.QuickSplitActivity;
import ue.edu.co.splitbill.ui.settle.SettlementActivity;

/**
 * Pantalla principal: el total del grupo, la lista de gastos y los accesos a las demas pantallas.
 *
 * El grupo es el grupo actual de la sesion; su nombre va en el titulo y al tocarlo se cambia de grupo. La lista siempre se lee de la base de datos del celular,
 * haya o no conexion; el SyncManager la mantiene al dia con el servidor y avisa por SyncListener
 * cuando termina, para recargarla y mostrar el estado de la sincronizacion.
 */
public class MainActivity extends BaseActivity
        implements ExpenseAdapter.OnExpenseDeleteListener, ExpenseAdapter.OnExpenseClickListener, SyncListener {

    /** Un gasto no se puede repartir si no hay al menos dos integrantes. */
    private static final int MIN_MEMBERS = 2;

    private TextView tvAppGreeting;
    private TextView tvTitle;
    private TextView tvSyncStatus;
    private TextView tvTotal;
    private TextView tvEmptyExpenses;
    private RecyclerView rvExpenses;
    private Button btnAddExpense;
    private Button btnSettle;
    private Button btnMembers;
    private Button btnQuickSplit;
    private Button btnSync;
    private Button btnLogout;

    private ExpenseAdapter expenseAdapter;
    private ExpenseRepository expenseRepository;
    private UserRepository userRepository;
    private SessionRepository sessionRepository;
    private GroupRepository groupRepository;
    private SyncManager syncManager;
    private NetworkMonitor networkMonitor;
    private String groupId;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initListeners() {
        this.btnAddExpense.setOnClickListener(this::openAddExpense);
        this.btnSettle.setOnClickListener(this::openSettlement);
        this.btnMembers.setOnClickListener(this::openMembers);
        this.btnQuickSplit.setOnClickListener(this::openQuickSplit);
        this.btnSync.setOnClickListener(this::syncAPI);
        this.btnLogout.setOnClickListener(this::confirmLogout);
        this.tvTitle.setOnClickListener(this::openGroups);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //pudo cambiar de grupo en la pantalla de grupos
        this.groupId = getServiceLocator().getSessionManager().getCurrentGroupId();
        loadGroupNameDB();
        //Al volver de registrar un gasto la lista se refresca sola
        listExpensesDB();
        loadTotalDB();
        //y se traen los cambios que otros integrantes hayan hecho en el servidor
        this.syncManager.addListener(this);
        this.syncManager.requestSync();
        suggestClaimOnce();
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
                R.string.btnSeeMembers, R.string.btnNotNow, () -> openMembers(null));
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
        for (String rejected : result.getRejectedMessages()) {
            showToast(getString(R.string.msgChangeRejected, rejected));
        }
        showSyncStatus(result);
        //el nombre del grupo pudo cambiar en el servidor
        loadGroupNameDB();
        listExpensesDB();
        loadTotalDB();
    }

    private void showSyncStatus(SyncResult result) {
        int pending = result.getPendingChanges();
        switch (result.getState()) {
            case SYNCED:
                this.tvSyncStatus.setText(R.string.tvSynced);
                break;
            case SERVER_ERROR:
                this.tvSyncStatus.setText(getResources().getQuantityString(
                        R.plurals.tvServerErrorPending, pending, pending));
                break;
            default:
                this.tvSyncStatus.setText(pending == 0 ? getString(R.string.tvOffline)
                        : getResources().getQuantityString(R.plurals.tvOfflinePending, pending, pending));
                break;
        }
    }

    /**
     * Antes de cerrar sesion se avisa cuantos cambios no alcanzaron a subirse, porque al cerrar se
     * borran los datos del celular.
     */
    private void confirmLogout(View view) {
        this.sessionRepository.countPendingChanges(new UiCallback<Integer>() {
            @Override
            protected void onData(Integer pending) {
                String message = getString(R.string.dlgLogoutMessage);
                if (pending > 0) {
                    message += "\n\n" + getResources().getQuantityString(R.plurals.dlgLogoutPending, pending, pending);
                }
                confirm(getString(R.string.dlgLogoutTitle), message, R.string.btnLogout, MainActivity.this::logout);
            }
        });
    }

    private void logout() {
        showLoading();
        this.sessionRepository.logout(new UiCallback<Boolean>() {
            @Override
            protected void onData(Boolean data) {
                goToLogin(false);
            }
        });
    }

    private void loadGroupNameDB() {
        this.groupRepository.getCurrentGroup(new UiCallback<Group>() {
            @Override
            protected void onData(Group data) {
                tvTitle.setText(data.getName());
            }
        });
    }

    private void listExpensesDB() {
        showLoading();
        this.expenseRepository.getActiveExpenses(this.groupId, new UiCallback<List<ExpenseListItem>>() {
            @Override
            protected void onData(List<ExpenseListItem> data) {
                expenseAdapter.setExpenses(data);
                tvEmptyExpenses.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void loadTotalDB() {
        this.expenseRepository.getTotalExpenses(this.groupId, new UiCallback<Money>() {
            @Override
            protected void onData(Money data) {
                tvTotal.setText(data.format());
            }
        });
    }

    @Override
    public void onExpenseClick(ExpenseListItem expense) {
        Intent intent = new Intent(this, ExpenseDetailActivity.class);
        intent.putExtra(ExpenseDetailActivity.EXTRA_EXPENSE_ID, expense.getExpenseId());
        startActivity(intent);
    }

    /** Un toque en la papelera no borra de una vez: primero se confirma. */
    @Override
    public void onExpenseDelete(final ExpenseListItem expense) {
        confirm(getString(R.string.dlgDeleteExpenseTitle, expense.getDescription()),
                getString(R.string.dlgDeleteExpenseMessage),
                R.string.btnDelete,
                () -> deleteExpenseDB(expense));
    }

    private void deleteExpenseDB(ExpenseListItem expense) {
        showLoading();
        this.expenseRepository.deleteExpense(expense.getExpenseId(), new UiCallback<Integer>() {
            @Override
            protected void onData(Integer data) {
                showToast(R.string.msgExpenseDeleted);
                listExpensesDB();
                loadTotalDB();
            }
        });
    }

    /** Antes de abrir el formulario se verifica que haya con quien repartir el gasto. */
    private void openAddExpense(View view) {
        this.userRepository.countActiveUsers(new UiCallback<Integer>() {
            @Override
            protected void onData(Integer data) {
                if (data < MIN_MEMBERS) {
                    //se lleva al usuario a crearlos, y desde alli puede seguir con el gasto
                    showToast(R.string.msgNeedTwoMembers);
                    Intent intent = new Intent(MainActivity.this, MembersActivity.class);
                    intent.putExtra(MembersActivity.EXTRA_CONTINUE_TO_EXPENSE, true);
                    startActivity(intent);
                    return;
                }
                startActivity(new Intent(MainActivity.this, AddExpenseActivity.class));
            }
        });
    }

    private void openGroups(View view) {
        startActivity(new Intent(this, GroupsActivity.class));
    }

    private void openSettlement(View view) {
        startActivity(new Intent(this, SettlementActivity.class));
    }

    private void openMembers(View view) {
        startActivity(new Intent(this, MembersActivity.class));
    }

    /** La cuenta rapida no necesita integrantes registrados: se abre siempre. */
    private void openQuickSplit(View view) {
        startActivity(new Intent(this, QuickSplitActivity.class));
    }

    @Override
    protected void initObjects() {
        this.tvAppGreeting = findViewById(R.id.tvAppGreeting);
        this.tvTitle = findViewById(R.id.tvTitle);
        this.tvSyncStatus = findViewById(R.id.tvSyncStatus);
        this.tvTotal = findViewById(R.id.tvTotal);
        this.tvEmptyExpenses = findViewById(R.id.tvEmptyExpenses);
        this.rvExpenses = findViewById(R.id.rvExpenses);
        this.btnAddExpense = findViewById(R.id.btnAddExpense);
        this.btnSettle = findViewById(R.id.btnSettle);
        this.btnMembers = findViewById(R.id.btnMembers);
        this.btnQuickSplit = findViewById(R.id.btnQuickSplit);
        this.btnSync = findViewById(R.id.btnSync);
        this.btnLogout = findViewById(R.id.btnLogout);

        this.groupId = getServiceLocator().getSessionManager().getCurrentGroupId();
        this.expenseRepository = getServiceLocator().getExpenseRepository();
        this.userRepository = getServiceLocator().getUserRepository();
        this.sessionRepository = getServiceLocator().getSessionRepository();
        this.groupRepository = getServiceLocator().getGroupRepository();
        this.syncManager = getServiceLocator().getSyncManager();
        this.networkMonitor = getServiceLocator().getNetworkMonitor();

        this.expenseAdapter = new ExpenseAdapter(
                getResources().getStringArray(R.array.splitTypes), this, this);
        this.rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        this.rvExpenses.setAdapter(this.expenseAdapter);

        this.tvTotal.setText(Money.ZERO.format());
        this.tvAppGreeting.setText(getString(R.string.tvGreetingUser,
                getServiceLocator().getSessionManager().getUserNames()));
        this.tvSyncStatus.setText(this.networkMonitor.isOnline() ? R.string.tvSyncing : R.string.tvOffline);
    }
}
