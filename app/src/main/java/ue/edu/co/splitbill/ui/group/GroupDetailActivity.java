package ue.edu.co.splitbill.ui.group;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.ui.chat.ChatActivity;
import ue.edu.co.splitbill.entity.Message;
import ue.edu.co.splitbill.dao.ExpenseListItem;
import ue.edu.co.splitbill.dao.GroupListItem;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.model.ExpenseRepository;
import ue.edu.co.splitbill.model.GroupRepository;
import ue.edu.co.splitbill.sync.NetworkMonitor;
import ue.edu.co.splitbill.sync.SyncListener;
import ue.edu.co.splitbill.sync.SyncManager;
import ue.edu.co.splitbill.sync.SyncResult;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.SyncStatusText;
import ue.edu.co.splitbill.ui.adapter.ExpenseAdapter;
import ue.edu.co.splitbill.ui.expense.ExpenseDetailActivity;
import ue.edu.co.splitbill.ui.settle.SettlementActivity;

/**
 * Un grupo: su total, el saldo de la persona en el, la lista de gastos y los accesos a liquidar y a
 * los integrantes. Es la que hasta la entrega 4 era la pantalla principal.
 *
 * El grupo es el grupo actual de la sesion; su nombre va en el titulo y al tocarlo se va a la lista
 * de grupos. La lista siempre se lee de la base de datos del celular, haya o no conexion; el
 * SyncManager la mantiene al dia con el servidor y avisa por SyncListener cuando termina.
 */
public class GroupDetailActivity extends BaseActivity
        implements ExpenseAdapter.OnExpenseClickListener, SyncListener {

    private TextView tvTitle;
    private TextView tvSyncStatus;
    private TextView tvTotal;
    private TextView tvGroupBalance;
    private TextView tvExpenseCount;
    private TextView tvEmptyExpenses;
    private RecyclerView rvExpenses;
    private ImageButton btnGroupSettings;
    private Button btnSettle;
    private Button btnMembers;
    private View cardChat;
    private TextView tvLastMessage;

    private ExpenseAdapter expenseAdapter;
    private ExpenseRepository expenseRepository;
    private GroupRepository groupRepository;
    private SyncManager syncManager;
    private NetworkMonitor networkMonitor;
    private String groupId;
    private String syncText;
    private String membersText;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_group_detail;
    }

    @Override
    protected int getNavItem() {
        return R.id.navGroups;
    }

    @Override
    protected void initListeners() {
        this.btnSettle.setOnClickListener(this::openSettlement);
        this.btnMembers.setOnClickListener(this::openGroupSettings);
        this.cardChat.setOnClickListener(this::openChat);
        this.btnGroupSettings.setOnClickListener(this::openGroupSettings);
        this.tvTitle.setOnClickListener(this::openGroups);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //pudo cambiar de grupo desde otra pantalla
        this.groupId = getServiceLocator().getSessionManager().getCurrentGroupId();
        loadGroupDB();
        //Al volver de registrar un gasto la lista se refresca sola
        listExpensesDB();
        loadLastMessageDB();
        this.syncManager.addListener(this);
        this.syncManager.requestSync();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.syncManager.removeListener(this);
    }

    @Override
    public void onSyncStarted() {
        this.syncText = getString(R.string.tvSyncing);
        showSubtitle();
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
        this.syncText = SyncStatusText.of(this, result);
        //el nombre del grupo o sus gastos pudieron cambiar en el servidor
        loadGroupDB();
        listExpensesDB();
    }

    private void showSubtitle() {
        this.tvSyncStatus.setText(this.membersText == null ? this.syncText
                : getString(R.string.tvGroupSubtitle, this.membersText, this.syncText));
    }

    /** Nombre, total, integrantes y saldo propio: salen de la misma consulta de la lista de grupos. */
    private void loadGroupDB() {
        this.groupRepository.getGroups(new UiCallback<List<GroupListItem>>() {
            @Override
            protected void onData(List<GroupListItem> data) {
                for (GroupListItem group : data) {
                    if (group.getGroupId().equals(groupId)) {
                        showGroup(group);
                        return;
                    }
                }
            }
        });
    }

    private void showGroup(GroupListItem group) {
        this.tvTitle.setText(group.getName());
        this.tvTotal.setText(group.getTotal().format());
        this.membersText = getResources().getQuantityString(R.plurals.tvGroupMembers,
                group.getMemberCount(), group.getMemberCount());
        showSubtitle();
        this.tvExpenseCount.setText(getResources().getQuantityString(R.plurals.tvExpenseCount,
                group.getExpenseCount(), group.getExpenseCount()));
        Money balance = group.getBalance();
        if (balance.isPositive()) {
            this.tvGroupBalance.setText(getString(R.string.tvGroupBalanceOwed, balance.format()));
        } else if (balance.isNegative()) {
            this.tvGroupBalance.setText(getString(R.string.tvGroupBalanceOwe, balance.abs().format()));
        } else {
            this.tvGroupBalance.setText(R.string.tvGroupBalanceSettled);
        }
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

    @Override
    public void onExpenseClick(ExpenseListItem expense) {
        Intent intent = new Intent(this, ExpenseDetailActivity.class);
        intent.putExtra(ExpenseDetailActivity.EXTRA_EXPENSE_ID, expense.getExpenseId());
        startActivity(intent);
    }

    private void openGroups(View view) {
        openTab(GroupsActivity.class);
    }

    /** "Diomar: ya pague" en la tarjeta del chat; si no hay mensajes, la invitacion a escribir. */
    private void loadLastMessageDB() {
        getServiceLocator().getChatRepository().getLastMessage(new UiCallback<Message>() {
            @Override
            protected void onData(Message data) {
                if (data == null) {
                    tvLastMessage.setText(R.string.tvChatCardHint);
                    return;
                }
                boolean mine = data.getSenderId() != null
                        && data.getSenderId().equals(getServiceLocator().getSessionManager().getUserId());
                tvLastMessage.setText(getString(R.string.tvLastMessage,
                        mine ? getString(R.string.tvYouShort) : data.getSenderNames(), data.getText()));
            }
        });
    }

    private void openChat(View view) {
        startActivity(new Intent(this, ChatActivity.class));
    }

    private void openSettlement(View view) {
        startActivity(new Intent(this, SettlementActivity.class));
    }

    /** Integrantes y nombre del grupo se editan en la misma pantalla. */
    private void openGroupSettings(View view) {
        startActivity(GroupFormActivity.editIntent(this));
    }

    @Override
    protected void initObjects() {
        this.tvTitle = findViewById(R.id.tvTitle);
        this.tvSyncStatus = findViewById(R.id.tvSyncStatus);
        this.tvTotal = findViewById(R.id.tvTotal);
        this.tvGroupBalance = findViewById(R.id.tvGroupBalance);
        this.tvExpenseCount = findViewById(R.id.tvExpenseCount);
        this.tvEmptyExpenses = findViewById(R.id.tvEmptyExpenses);
        this.rvExpenses = findViewById(R.id.rvExpenses);
        this.btnGroupSettings = findViewById(R.id.btnGroupSettings);
        this.btnSettle = findViewById(R.id.btnSettle);
        this.btnMembers = findViewById(R.id.btnMembers);
        this.cardChat = findViewById(R.id.cardChat);
        this.tvLastMessage = findViewById(R.id.tvLastMessage);

        this.groupId = getServiceLocator().getSessionManager().getCurrentGroupId();
        this.expenseRepository = getServiceLocator().getExpenseRepository();
        this.groupRepository = getServiceLocator().getGroupRepository();
        this.syncManager = getServiceLocator().getSyncManager();
        this.networkMonitor = getServiceLocator().getNetworkMonitor();

        this.expenseAdapter = new ExpenseAdapter(getResources().getStringArray(R.array.splitTypes), this, false);
        this.rvExpenses.setAdapter(this.expenseAdapter);

        this.tvTotal.setText(Money.ZERO.format());
        this.syncText = getString(this.networkMonitor.isOnline() ? R.string.tvSyncing : R.string.tvOffline);
        showSubtitle();
    }
}
