package ue.edu.co.splitbill.ui.group;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.dao.GroupListItem;
import ue.edu.co.splitbill.model.GroupRepository;
import ue.edu.co.splitbill.sync.SyncListener;
import ue.edu.co.splitbill.sync.SyncManager;
import ue.edu.co.splitbill.sync.SyncResult;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.adapter.GroupAdapter;
import ue.edu.co.splitbill.ui.quick.SavedQuickSplitsActivity;

/**
 * Pestana Grupos: todos los grupos de la persona, con su total y su saldo en cada uno.
 *
 * Tocar un grupo lo vuelve el grupo actual y lo abre. El menu de la tarjeta lleva a editarlo
 * (nombre e integrantes) y "Nuevo grupo" abre el formulario para crearlo con su gente.
 */
public class GroupsActivity extends BaseActivity implements GroupAdapter.OnGroupListener, SyncListener {

    private View cardSavedQuickSplits;
    private TextView tvSavedQuickCount;
    private RecyclerView rvGroups;
    private Button btnNewGroup;

    private GroupAdapter groupAdapter;
    private GroupRepository groupRepository;
    private SyncManager syncManager;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_groups;
    }

    @Override
    protected int getNavItem() {
        return R.id.navGroups;
    }

    @Override
    protected void initListeners() {
        this.btnNewGroup.setOnClickListener(this::openNewGroup);
        this.cardSavedQuickSplits.setOnClickListener(this::openSavedQuickSplits);
    }

    @Override
    protected void onResume() {
        super.onResume();
        listGroupsDB();
        countSavedQuickSplitsDB();
        //si otro integrante lo agrego a un grupo desde su celular, aparece al sincronizar
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
        //no se muestra nada: la lista ya esta en pantalla
    }

    @Override
    public void onSyncFinished(SyncResult result) {
        if (isAlive()) {
            listGroupsDB();
            countSavedQuickSplitsDB();
        }
    }

    /** "3 guardadas, sin grupo", o como crear la primera. */
    private void countSavedQuickSplitsDB() {
        getServiceLocator().getQuickSplitRepository().countSavedQuickSplits(new UiCallback<Integer>() {
            @Override
            protected void onData(Integer data) {
                tvSavedQuickCount.setText(data == 0 ? getString(R.string.tvSavedQuickNone)
                        : getResources().getQuantityString(R.plurals.tvSavedQuickCount, data, data));
            }
        });
    }

    private void openSavedQuickSplits(View view) {
        startActivity(new Intent(this, SavedQuickSplitsActivity.class));
    }

    private void listGroupsDB() {
        this.groupRepository.getGroups(new UiCallback<List<GroupListItem>>() {
            @Override
            protected void onData(List<GroupListItem> data) {
                groupAdapter.setGroups(data, groupRepository.getCurrentGroupId());
            }
        });
    }

    @Override
    public void onGroupClick(GroupListItem group) {
        if (!group.getGroupId().equals(this.groupRepository.getCurrentGroupId())) {
            this.groupRepository.switchGroup(group.getGroupId());
            showToast(getString(R.string.msgGroupSwitched, group.getName()));
        }
        startActivity(new Intent(this, GroupDetailActivity.class));
    }

    /** El menu de la tarjeta abre el grupo para editarlo: nombre e integrantes. */
    @Override
    public void onGroupMenu(GroupListItem group, View anchor) {
        this.groupRepository.switchGroup(group.getGroupId());
        startActivity(GroupFormActivity.editIntent(this));
    }

    private void openNewGroup(View view) {
        startActivity(new Intent(this, GroupFormActivity.class));
    }

    @Override
    protected void initObjects() {
        this.cardSavedQuickSplits = findViewById(R.id.cardSavedQuickSplits);
        this.tvSavedQuickCount = findViewById(R.id.tvSavedQuickCount);
        this.rvGroups = findViewById(R.id.rvGroups);
        this.btnNewGroup = findViewById(R.id.btnNewGroup);

        this.groupRepository = getServiceLocator().getGroupRepository();
        this.syncManager = getServiceLocator().getSyncManager();
        this.groupAdapter = new GroupAdapter(this, false);
        this.rvGroups.setAdapter(this.groupAdapter);
    }
}
