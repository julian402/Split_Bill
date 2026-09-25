package ue.edu.co.splitbill.ui.group;

import android.content.Context;
import android.content.Intent;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.entity.Group;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.model.GroupRepository;
import ue.edu.co.splitbill.model.UserRepository;
import ue.edu.co.splitbill.permission.PermissionManager;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.adapter.MemberAdapter;
import ue.edu.co.splitbill.ui.contacts.ContactsActivity;
import ue.edu.co.splitbill.ui.expense.AddExpenseActivity;

/**
 * Nuevo grupo e integrantes: una sola pantalla con dos modos.
 *
 * - Crear (por defecto): se escribe el nombre y se arma la lista de gente (a mano o desde los
 *   contactos). Nada se guarda hasta tocar "Guardar grupo"; entonces el grupo y sus integrantes se
 *   guardan juntos en una transaccion y se abre el grupo.
 * - Editar (editIntent): el grupo actual. Cada integrante que se agrega o se quita se guarda de una
 *   vez, como en la entrega 1; el nombre solo lo puede cambiar quien creo el grupo.
 *
 * Grupos compartidos: "Invitar por email" agrega a una persona con su propia cuenta, y "Vincular con
 * su cuenta" une a alguien que se habia agregado solo por nombre con la cuenta de su email. En los dos
 * casos esa persona ve el grupo en su celular, con los mismos gastos y saldos. Necesitan conexion; en
 * modo crear, las invitaciones se envian justo despues de guardar el grupo.
 */
public class GroupFormActivity extends BaseActivity
        implements MemberAdapter.OnMemberDeleteListener, MemberAdapter.OnMemberLinkListener {

    /**
     * Llega en true cuando el usuario queria registrar un gasto y le faltaban integrantes. En ese caso,
     * apenas haya dos o mas, el boton de abajo pasa a "Continuar con el gasto" y lo devuelve al formulario.
     */
    public static final String EXTRA_CONTINUE_TO_EXPENSE = "extraContinueToExpense";
    /** Editar el grupo actual en vez de crear uno nuevo. */
    public static final String EXTRA_EDIT = "extraEdit";

    private TextView tvFormTitle;
    private TextView tvFormSubtitle;
    private TextView tvHeroTitle;
    private TextView tvHeroSubtitle;
    private ImageView ivHeroDecoIcon;
    private Button btnFocusMember;
    private Button btnAddFromContacts;
    private TextInputLayout tilGroupName;
    private EditText etGroupName;
    private TextView tvMemberCount;
    private TextView tvReady;
    private EditText etMemberNames;
    private EditText etMemberPhone;
    private Button btnSaveMember;
    private Button btnInviteByEmail;
    private Button btnClaimMember;
    private RecyclerView rvMembers;
    private Button btnSaveGroup;

    private MemberAdapter memberAdapter;
    private GroupRepository groupRepository;
    private UserRepository userRepository;
    private PermissionManager permissionManager;
    private ActivityResultLauncher<Intent> contactsLauncher;
    private User user;
    private boolean editMode;
    private Group group;

    /** Modo crear: la gente que se va agregando, que se guarda junto con el grupo. */
    private final List<User> pendingMembers = new ArrayList<>();
    /** Modo crear: emails de personas con cuenta, que se invitan apenas se guarda el grupo. */
    private final List<String> pendingInvites = new ArrayList<>();
    /** Integrantes agregados por nombre (sin cuenta): entre ellos puede estar quien inicio sesion. */
    private final List<User> claimableMembers = new ArrayList<>();
    private int memberCount;

    /** Abre el grupo actual para editarlo: nombre e integrantes. */
    public static Intent editIntent(Context context) {
        Intent intent = new Intent(context, GroupFormActivity.class);
        intent.putExtra(EXTRA_EDIT, true);
        return intent;
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_group_form;
    }

    @Override
    protected int getNavItem() {
        return R.id.navGroups;
    }

    @Override
    protected void initListeners() {
        this.btnFocusMember.setOnClickListener(this::focusMemberForm);
        this.btnAddFromContacts.setOnClickListener(this::openContacts);
        this.btnSaveMember.setOnClickListener(this::addMemberDB);
        this.btnInviteByEmail.setOnClickListener(this::askInviteEmail);
        this.btnClaimMember.setOnClickListener(this::pickMemberToClaim);
        this.btnSaveGroup.setOnClickListener(this::saveGroupDB);

        //si escribio algo y no lo guardo, se pregunta antes de salir
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!hasUnsavedChanges()) {
                    finish();
                    return;
                }
                confirm(getString(R.string.dlgDiscardTitle),
                        getString(editMode ? R.string.dlgDiscardMessage : R.string.dlgDiscardGroupMessage),
                        R.string.btnDiscard, GroupFormActivity.this::finish);
            }
        });
    }

    private boolean hasUnsavedChanges() {
        boolean typedMember = !this.etMemberNames.getText().toString().trim().isEmpty()
                || !this.etMemberPhone.getText().toString().trim().isEmpty();
        if (this.editMode) {
            return typedMember;
        }
        return typedMember || !this.pendingMembers.isEmpty() || !this.pendingInvites.isEmpty()
                || !this.etGroupName.getText().toString().trim().isEmpty();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.editMode) {
            loadGroupDB();
            listMembersDB();
        }
    }

    // ------------------------------------------------------------------ modo editar

    private void loadGroupDB() {
        this.groupRepository.getCurrentGroup(new UiCallback<Group>() {
            @Override
            protected void onData(Group data) {
                group = data;
                if (etGroupName.getText().toString().isEmpty()) {
                    etGroupName.setText(data.getName());
                }
                //el servidor solo deja cambiar el nombre a quien creo el grupo
                boolean isOwner = data.getOwnerId() == null
                        || data.getOwnerId().equals(groupRepository.getCurrentUserId());
                tilGroupName.setEnabled(isOwner);
                tilGroupName.setHelperText(isOwner ? null : getString(R.string.tvOnlyOwnerRenamesHelper));
            }
        });
    }

    private void listMembersDB() {
        showLoading();
        this.userRepository.getActiveUsers(new UiCallback<List<User>>() {
            @Override
            protected void onData(List<User> data) {
                memberAdapter.setMembers(data, group == null ? null : group.getOwnerId());
                showMemberCount(data.size());

                claimableMembers.clear();
                String currentUserId = userRepository.getCurrentUserId();
                for (User member : data) {
                    boolean withoutAccount = member.getEmail() == null || member.getEmail().trim().isEmpty();
                    if (withoutAccount && !member.getId().equals(currentUserId)) {
                        claimableMembers.add(member);
                    }
                }
                btnClaimMember.setVisibility(claimableMembers.isEmpty() ? View.GONE : View.VISIBLE);
            }
        });
    }

    // ------------------------------------------------------------------ integrantes

    //metodo para insertar en la db (modo editar) o en la lista del formulario (modo crear)
    private void addMemberDB(View view) {
        getData();
        if (!this.editMode) {
            addPendingMember(this.user);
            return;
        }
        showLoading();
        this.userRepository.insertUser(this.user, new UiCallback<User>() {
            @Override
            protected void onData(User data) {
                clearFields();
                showToast(R.string.msgMemberSaved);
                listMembersDB();
            }
        });
    }

    //metodo para capturar la data del activity
    private void getData() {
        String names = this.etMemberNames.getText().toString();
        String phone = this.etMemberPhone.getText().toString();
        this.user = new User(names.trim(), null, phone.trim());
    }

    /** Modo crear: se valida igual que al guardar, pero la persona queda en la lista hasta el final. */
    private void addPendingMember(User member) {
        try {
            member.validar();
        } catch (IllegalArgumentException e) {
            showToast(e.getMessage());
            return;
        }
        for (User pending : this.pendingMembers) {
            if (pending.getNames().equalsIgnoreCase(member.getNames())) {
                showToast(getString(R.string.msgMemberAlreadyInList, member.getNames()));
                return;
            }
        }
        this.pendingMembers.add(member);
        clearFields();
        showPendingMembers();
    }

    /** La persona de la sesion encabeza la lista como Administrador; luego, la gente agregada. */
    private void showPendingMembers() {
        List<User> list = new ArrayList<>();
        User me = new User(getServiceLocator().getSessionManager().getUserNames(),
                getServiceLocator().getSessionManager().getUserEmail(), null);
        me.setId(this.groupRepository.getCurrentUserId());
        list.add(me);
        list.addAll(this.pendingMembers);
        for (String email : this.pendingInvites) {
            //todavia no se sabe su nombre: se muestra el email hasta que el servidor lo diga
            list.add(new User(email, email, null));
        }
        this.memberAdapter.setMembers(list, me.getId());
        showMemberCount(list.size());
    }

    /** "3 integrantes" y, con dos o mas, "Listo para empezar". */
    private void showMemberCount(int count) {
        this.memberCount = count;
        this.tvMemberCount.setText(getResources().getQuantityString(R.plurals.tvGroupMembers, count, count));
        boolean ready = count >= MIN_MEMBERS;
        this.tvReady.setText(ready ? R.string.tvReadyToStart : R.string.tvNeedMoreMembers);
        this.tvReady.setTextColor(ContextCompat.getColor(this, ready ? R.color.colorCreditor : R.color.colorTextSecondary));
        if (this.editMode) {
            boolean canContinue = getIntent().getBooleanExtra(EXTRA_CONTINUE_TO_EXPENSE, false) && ready;
            this.btnSaveGroup.setText(canContinue ? R.string.btnContinueExpense : R.string.btnSaveChanges);
        }
    }

    /** Un toque en la papelera no borra de una vez: primero se confirma. */
    @Override
    public void onMemberDelete(final User member) {
        if (!this.editMode) {
            //todavia no esta guardado: se quita de la lista sin preguntar
            if (!this.pendingMembers.remove(member)) {
                this.pendingInvites.remove(member.getEmail());
            }
            showPendingMembers();
            return;
        }
        confirm(getString(R.string.dlgDeleteMemberTitle, member.getNames()),
                getString(R.string.dlgDeleteMemberMessage),
                R.string.btnDelete,
                () -> deleteMemberDB(member));
    }

    private void deleteMemberDB(User member) {
        showLoading();
        this.userRepository.deleteUser(member.getId(), new UiCallback<Integer>() {
            @Override
            protected void onData(Integer data) {
                showToast(R.string.msgMemberDeleted);
                listMembersDB();
            }
        });
    }

    private void focusMemberForm(View view) {
        this.etMemberNames.requestFocus();
        this.etMemberNames.post(() -> findViewById(R.id.svForm).scrollTo(0, (int) this.etMemberNames.getY()));
    }

    /**
     * La agenda es informacion privada: se pide el permiso solo cuando el usuario toca el boton, y
     * con el permiso concedido se abre el selector. En modo crear, el selector devuelve lo elegido.
     */
    private void openContacts(View view) {
        this.permissionManager.request(PermissionManager.CONTACTS, R.string.msgContactsRationale, () -> {
            Intent intent = new Intent(this, ContactsActivity.class);
            if (this.editMode) {
                startActivity(intent);
                return;
            }
            ArrayList<String> names = new ArrayList<>();
            ArrayList<String> phones = new ArrayList<>();
            for (User pending : this.pendingMembers) {
                names.add(pending.getNames());
                phones.add(pending.getPhone() == null ? "" : pending.getPhone());
            }
            intent.putExtra(ContactsActivity.EXTRA_RETURN_SELECTION, true);
            intent.putStringArrayListExtra(ContactsActivity.EXTRA_NAMES, names);
            intent.putStringArrayListExtra(ContactsActivity.EXTRA_PHONES, phones);
            this.contactsLauncher.launch(intent);
        });
    }

    /** Los contactos elegidos en modo crear pasan a la lista del formulario. */
    private void onContactsPicked(ActivityResult result) {
        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
            return;
        }
        ArrayList<String> names = result.getData().getStringArrayListExtra(ContactsActivity.EXTRA_NAMES);
        ArrayList<String> phones = result.getData().getStringArrayListExtra(ContactsActivity.EXTRA_PHONES);
        if (names == null || phones == null) {
            return;
        }
        for (int i = 0; i < names.size(); i++) {
            User contact = new User(names.get(i).trim(), null, phones.get(i));
            if (contact.getNames().length() >= 2) {
                this.pendingMembers.add(contact);
            }
        }
        showPendingMembers();
        showToast(getResources().getQuantityString(R.plurals.msgContactsAdded, names.size(), names.size()));
    }

    // ------------------------------------------------------------------ guardar

    private void saveGroupDB(View view) {
        if (!this.editMode) {
            createGroupDB();
            return;
        }
        String name = this.etGroupName.getText().toString().trim();
        boolean renamed = this.group != null && this.tilGroupName.isEnabled() && !name.equals(this.group.getName());
        if (!renamed) {
            finishEdit();
            return;
        }
        showLoading();
        this.groupRepository.renameGroup(this.group.getId(), name, new UiCallback<Integer>() {
            @Override
            protected void onData(Integer data) {
                showToast(R.string.msgGroupRenamed);
                finishEdit();
            }
        });
    }

    /** Si venia de "Agregar gasto" y ya hay con quien repartir, vuelve al formulario del gasto. */
    private void finishEdit() {
        boolean canContinue = getIntent().getBooleanExtra(EXTRA_CONTINUE_TO_EXPENSE, false)
                && this.memberCount >= MIN_MEMBERS;
        if (canContinue) {
            Intent intent = new Intent(this, AddExpenseActivity.class);
            intent.putExtras(getIntent());
            intent.removeExtra(EXTRA_CONTINUE_TO_EXPENSE);
            intent.removeExtra(EXTRA_EDIT);
            startActivity(intent);
        }
        finish();
    }

    /** El grupo y su gente se guardan juntos; luego se abre el grupo nuevo, ya como grupo actual. */
    private void createGroupDB() {
        showLoading();
        this.btnSaveGroup.setEnabled(false);
        this.groupRepository.createGroup(this.etGroupName.getText().toString(), this.pendingMembers,
                new UiCallback<Group>() {
                    @Override
                    protected void onData(Group data) {
                        showToast(getString(R.string.msgGroupCreated, data.getName()));
                        sendPendingInvites(0);
                    }

                    @Override
                    public void onError(String message) {
                        super.onError(message);
                        btnSaveGroup.setEnabled(true);
                    }
                });
    }

    /**
     * Modo crear: las invitaciones por email se envian una tras otra, ya con el grupo guardado. Si una
     * falla (sin conexion, o nadie tiene ese email), se avisa y se sigue; se puede repetir desde
     * Integrantes. Al terminar se abre el grupo.
     */
    private void sendPendingInvites(final int index) {
        if (index >= this.pendingInvites.size()) {
            startActivity(new Intent(this, GroupDetailActivity.class));
            finish();
            return;
        }
        final String email = this.pendingInvites.get(index);
        showLoading();
        this.userRepository.inviteByEmail(email, new UiCallback<User>() {
            @Override
            protected void onData(User data) {
                showToast(getString(R.string.msgInviteDone, data.getNames()));
                sendPendingInvites(index + 1);
            }

            @Override
            public void onError(String message) {
                hideLoading();
                showToast(getString(R.string.msgInviteFailed, email, message));
                sendPendingInvites(index + 1);
            }
        });
    }

    // ------------------------------------------------------------------ invitar y vincular por email

    private void askInviteEmail(View view) {
        askEmail(getString(R.string.dlgInviteTitle), getString(R.string.dlgInviteMessage), R.string.btnInvite,
                this::inviteByEmail);
    }

    /** Modo editar: se invita de una vez. Modo crear: queda en la lista hasta guardar el grupo. */
    private void inviteByEmail(String email) {
        if (!this.editMode) {
            if (this.pendingInvites.contains(email)) {
                showToast(getString(R.string.msgInviteAlreadyInList, email));
                return;
            }
            this.pendingInvites.add(email);
            showPendingMembers();
            showToast(getString(R.string.msgInvitePending, email));
            return;
        }
        showLoading();
        this.userRepository.inviteByEmail(email, new UiCallback<User>() {
            @Override
            protected void onData(User data) {
                showToast(getString(R.string.msgInviteDone, data.getNames()));
                listMembersDB();
            }
        });
    }

    @Override
    public void onMemberLink(final User member) {
        askEmail(getString(R.string.dlgLinkTitle, member.getNames()),
                getString(R.string.dlgLinkMessage, member.getNames()), R.string.btnLink,
                email -> linkMemberAPI(member, email));
    }

    private void linkMemberAPI(final User member, String email) {
        showLoading();
        this.userRepository.linkMember(member.getId(), email, new UiCallback<User>() {
            @Override
            protected void onData(User data) {
                showToast(getString(R.string.msgLinkDone, data.getNames()));
                listMembersDB();
            }
        });
    }

    /** Lo que se hace con el email, una vez validado. */
    private interface OnEmailListener {
        void onEmail(String email);
    }

    /** Dialogo con un campo de email; si no es valido, avisa y no hace nada. */
    private void askEmail(String title, String message, int positiveButtonResourceId, final OnEmailListener listener) {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_email, null);
        final EditText etEmail = content.findViewById(R.id.etDialogEmail);
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setView(content)
                .setNegativeButton(R.string.btnCancel, null)
                .setPositiveButton(positiveButtonResourceId, (dialog, which) -> {
                    String email = etEmail.getText().toString().trim().toLowerCase(Locale.ROOT);
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        showToast(R.string.msgInvalidEmail);
                        return;
                    }
                    listener.onEmail(email);
                })
                .show();
    }

    // ------------------------------------------------------------------ "Soy yo"

    /** Primer paso de "Soy yo": elegir cual de los integrantes sin cuenta es la persona. */
    private void pickMemberToClaim(View view) {
        String[] names = new String[this.claimableMembers.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = this.claimableMembers.get(i).getNames();
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dlgPickClaimTitle)
                .setItems(names, (dialog, which) -> confirmClaim(this.claimableMembers.get(which)))
                .setNegativeButton(R.string.btnCancel, null)
                .show();
    }

    /** Segundo paso: se confirma, porque mueve gastos de todo el grupo. */
    private void confirmClaim(final User member) {
        confirm(getString(R.string.dlgClaimTitle, member.getNames()),
                getString(R.string.dlgClaimMessage, member.getNames()),
                R.string.btnClaimConfirm,
                () -> claimMemberAPI(member));
    }

    private void claimMemberAPI(final User member) {
        showLoading();
        this.userRepository.claimMember(member.getId(), new UiCallback<Boolean>() {
            @Override
            protected void onData(Boolean data) {
                showToast(getString(R.string.msgClaimDone, member.getNames()));
                listMembersDB();
            }
        });
    }

    private void clearFields() {
        this.etMemberNames.setText("");
        this.etMemberPhone.setText("");
        this.etMemberNames.requestFocus();
    }

    /** Los textos cambian segun el modo: crear un grupo o editar el actual. */
    private void showMode() {
        this.tvFormTitle.setText(this.editMode ? R.string.tvTitleEditGroup : R.string.tvTitleNewGroup);
        this.tvFormSubtitle.setText(this.editMode ? R.string.tvEditGroupSubtitle : R.string.tvNewGroupSubtitle);
        this.tvHeroTitle.setText(this.editMode ? R.string.tvEditYourGroup : R.string.tvCreateYourGroup);
        this.tvHeroSubtitle.setText(this.editMode ? R.string.tvEditYourGroupHint : R.string.tvCreateYourGroupHint);
        this.btnSaveGroup.setText(this.editMode ? R.string.btnSaveChanges : R.string.btnSaveGroup);
        this.ivHeroDecoIcon.setImageResource(R.drawable.ic_group);
        if (!this.editMode) {
            showPendingMembers();
        }
    }

    @Override
    protected void initObjects() {
        this.tvFormTitle = findViewById(R.id.tvFormTitle);
        this.tvFormSubtitle = findViewById(R.id.tvFormSubtitle);
        this.tvHeroTitle = findViewById(R.id.tvHeroTitle);
        this.tvHeroSubtitle = findViewById(R.id.tvHeroSubtitle);
        this.ivHeroDecoIcon = findViewById(R.id.ivHeroDecoIcon);
        this.btnFocusMember = findViewById(R.id.btnFocusMember);
        this.btnAddFromContacts = findViewById(R.id.btnAddFromContacts);
        this.tilGroupName = findViewById(R.id.tilGroupName);
        this.etGroupName = findViewById(R.id.etGroupName);
        this.tvMemberCount = findViewById(R.id.tvMemberCount);
        this.tvReady = findViewById(R.id.tvReady);
        this.etMemberNames = findViewById(R.id.etMemberNames);
        this.etMemberPhone = findViewById(R.id.etMemberPhone);
        this.btnSaveMember = findViewById(R.id.btnSaveMember);
        this.btnInviteByEmail = findViewById(R.id.btnInviteByEmail);
        this.btnClaimMember = findViewById(R.id.btnClaimMember);
        this.rvMembers = findViewById(R.id.rvMembers);
        this.btnSaveGroup = findViewById(R.id.btnSaveGroup);

        this.editMode = getIntent().getBooleanExtra(EXTRA_EDIT, false)
                || getIntent().getBooleanExtra(EXTRA_CONTINUE_TO_EXPENSE, false);
        this.groupRepository = getServiceLocator().getGroupRepository();
        this.userRepository = getServiceLocator().getUserRepository();
        this.permissionManager = new PermissionManager(this);
        this.contactsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), this::onContactsPicked);
        this.memberAdapter = new MemberAdapter(this, this.userRepository.getCurrentUserId());
        if (this.editMode) {
            //solo un grupo que ya existe tiene integrantes en el servidor para vincular
            this.memberAdapter.setOnMemberLinkListener(this);
        }
        this.rvMembers.setAdapter(this.memberAdapter);
        showMode();
    }
}
