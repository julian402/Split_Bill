package ue.edu.co.splitbill.ui.group;

import android.content.Intent;
import android.view.View;
import android.widget.EditText;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.dao.GroupListItem;
import ue.edu.co.splitbill.entity.Group;
import ue.edu.co.splitbill.model.GroupRepository;
import ue.edu.co.splitbill.sync.SyncListener;
import ue.edu.co.splitbill.sync.SyncManager;
import ue.edu.co.splitbill.sync.SyncResult;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.adapter.GroupAdapter;

/**
 * Los grupos de la persona: el paseo, la casa, la oficina... Cada uno tiene sus propios integrantes,
 * gastos y liquidacion.
 *
 * Tocar un grupo lo vuelve el grupo actual y regresa a la pantalla principal, que ya muestra ese
 * grupo. Con un toque largo se le cambia el nombre (solo quien lo creo).
 */
public class GroupsActivity extends BaseActivity implements GroupAdapter.OnGroupListener, SyncListener {

    private RecyclerView rvGroups;
    private ExtendedFloatingActionButton btnNewGroup;

    private GroupAdapter groupAdapter;
    private GroupRepository groupRepository;
    private SyncManager syncManager;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_groups;
    }

    @Override
    protected void initListeners() {
        this.btnNewGroup.setOnClickListener(this::showNewGroupDialog);
    }

    @Override
    protected void onResume() {
        super.onResume();
        listGroupsDB();
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
        }
    }

    private void listGroupsDB() {
        showLoading();
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
        goToMain();
    }

    /** Solo quien creo el grupo le puede cambiar el nombre (el servidor tambien lo exige). */
    @Override
    public void onGroupLongClick(final GroupListItem group) {
        boolean isOwner = group.getOwnerId() == null
                || group.getOwnerId().equals(this.groupRepository.getCurrentUserId());
        if (!isOwner) {
            showToast(R.string.msgOnlyOwnerRenames);
            return;
        }
        showNameDialog(R.string.dlgRenameGroupTitle, R.string.btnSave, group.getName(),
                name -> renameGroupDB(group, name));
    }

    private void showNewGroupDialog(View view) {
        showNameDialog(R.string.dlgNewGroupTitle, R.string.btnCreate, "", this::createGroupDB);
    }

    /** Lo que se hace con el nombre escrito en el dialogo. */
    private interface OnNameListener {
        void onName(String name);
    }

    private void showNameDialog(int title, int positiveButton, String currentName, final OnNameListener listener) {
        View content = getLayoutInflater().inflate(R.layout.dialog_group_name, null);
        final EditText etGroupName = content.findViewById(R.id.etGroupName);
        etGroupName.setText(currentName);
        etGroupName.setSelection(currentName.length());
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(content)
                .setNegativeButton(R.string.btnCancel, null)
                .setPositiveButton(positiveButton, (dialog, which) ->
                        listener.onName(etGroupName.getText().toString()))
                .show();
        etGroupName.requestFocus();
    }

    /** El grupo nuevo queda como actual: se vuelve a la principal y de una vez se agregan integrantes. */
    private void createGroupDB(String name) {
        showLoading();
        this.groupRepository.createGroup(name, new UiCallback<Group>() {
            @Override
            protected void onData(Group data) {
                showToast(getString(R.string.msgGroupCreated, data.getName()));
                goToMain();
                startActivity(new Intent(GroupsActivity.this, MembersActivity.class));
            }
        });
    }

    private void renameGroupDB(GroupListItem group, String name) {
        showLoading();
        this.groupRepository.renameGroup(group.getGroupId(), name, new UiCallback<Integer>() {
            @Override
            protected void onData(Integer data) {
                showToast(R.string.msgGroupRenamed);
                listGroupsDB();
            }
        });
    }

    /** Vuelve a la principal que ya estaba abierta, sin apilar otra. */
    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void initObjects() {
        this.rvGroups = findViewById(R.id.rvGroups);
        this.btnNewGroup = findViewById(R.id.btnNewGroup);

        this.groupRepository = getServiceLocator().getGroupRepository();
        this.syncManager = getServiceLocator().getSyncManager();
        this.groupAdapter = new GroupAdapter(this);
        this.rvGroups.setLayoutManager(new LinearLayoutManager(this));
        this.rvGroups.setAdapter(this.groupAdapter);
    }
}
