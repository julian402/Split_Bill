package ue.edu.co.splitbill.ui.group;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.model.UserRepository;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.adapter.MemberAdapter;
import ue.edu.co.splitbill.ui.expense.AddExpenseActivity;

/**
 * Pantalla de integrantes del grupo: registrar, listar y dar de baja.
 *
 * Es el formulario mas sencillo de la aplicacion y sirve para verificar que toda la cadena funciona:
 * la pantalla llama al repositorio, el repositorio trabaja en otro hilo, Room escribe en SQLite y la
 * respuesta regresa al hilo principal para actualizar la lista.
 */
public class MembersActivity extends BaseActivity implements MemberAdapter.OnMemberDeleteListener {

    /**
     * Llega en true cuando el usuario queria registrar un gasto y le faltaban integrantes. En ese caso,
     * apenas haya dos o mas, aparece el boton "Continuar con el gasto" que lo devuelve al formulario.
     */
    public static final String EXTRA_CONTINUE_TO_EXPENSE = "extraContinueToExpense";

    /** Un gasto no se puede repartir si no hay al menos dos integrantes. */
    private static final int MIN_MEMBERS = 2;

    private EditText etMemberNames;
    private EditText etMemberPhone;
    private Button btnSaveMember;
    private Button btnClear;
    private Button btnContinueExpense;
    private Button btnClaimMember;
    private TextView tvEmptyMembers;
    private RecyclerView rvMembers;

    private MemberAdapter memberAdapter;
    private UserRepository userRepository;
    private User user;

    /** Integrantes agregados por nombre (sin cuenta): entre ellos puede estar quien inicio sesion. */
    private final List<User> claimableMembers = new ArrayList<>();

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_members;
    }

    @Override
    protected void initListeners() {
        this.btnSaveMember.setOnClickListener(this::addMemberDB);
        this.btnClear.setOnClickListener(this::clearFieldsDB);
        this.btnContinueExpense.setOnClickListener(this::continueToExpense);
        this.btnClaimMember.setOnClickListener(this::pickMemberToClaim);

        //si escribio un nombre y no lo guardo, se pregunta antes de salir
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                boolean typed = !etMemberNames.getText().toString().trim().isEmpty()
                        || !etMemberPhone.getText().toString().trim().isEmpty();
                if (!typed) {
                    finish();
                    return;
                }
                confirm(getString(R.string.dlgDiscardTitle), getString(R.string.dlgDiscardMessage),
                        R.string.btnDiscard, MembersActivity.this::finish);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        listMembersDB();
    }

    //metodo para insertar en la db
    private void addMemberDB(View view) {
        getData();
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

    private void listMembersDB() {
        showLoading();
        this.userRepository.getActiveUsers(new UiCallback<List<User>>() {
            @Override
            protected void onData(List<User> data) {
                memberAdapter.setMembers(data);
                tvEmptyMembers.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
                boolean canContinue = getIntent().getBooleanExtra(EXTRA_CONTINUE_TO_EXPENSE, false)
                        && data.size() >= MIN_MEMBERS;
                btnContinueExpense.setVisibility(canContinue ? View.VISIBLE : View.GONE);

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

    /** Vuelve al formulario del gasto con lo que el usuario ya traia (descripcion y monto). */
    private void continueToExpense(View view) {
        Intent intent = new Intent(this, AddExpenseActivity.class);
        intent.putExtras(getIntent());
        intent.removeExtra(EXTRA_CONTINUE_TO_EXPENSE);
        startActivity(intent);
        finish();
    }

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
    private void confirmClaim(final User user) {
        confirm(getString(R.string.dlgClaimTitle, user.getNames()),
                getString(R.string.dlgClaimMessage, user.getNames()),
                R.string.btnClaimConfirm,
                () -> claimMemberAPI(user));
    }

    private void claimMemberAPI(final User user) {
        showLoading();
        this.userRepository.claimMember(user.getId(), new UiCallback<Boolean>() {
            @Override
            protected void onData(Boolean data) {
                showToast(getString(R.string.msgClaimDone, user.getNames()));
                listMembersDB();
            }
        });
    }

    /** Un toque en la papelera no borra de una vez: primero se confirma. */
    @Override
    public void onMemberDelete(final User user) {
        confirm(getString(R.string.dlgDeleteMemberTitle, user.getNames()),
                getString(R.string.dlgDeleteMemberMessage),
                R.string.btnDelete,
                () -> deleteMemberDB(user));
    }

    private void deleteMemberDB(User user) {
        showLoading();
        this.userRepository.deleteUser(user.getId(), new UiCallback<Integer>() {
            @Override
            protected void onData(Integer data) {
                showToast(R.string.msgMemberDeleted);
                listMembersDB();
            }
        });
    }

    private void clearFieldsDB(View view) {
        clearFields();
    }

    private void clearFields() {
        this.etMemberNames.setText("");
        this.etMemberPhone.setText("");
        this.etMemberNames.requestFocus();
    }

    @Override
    protected void initObjects() {
        this.etMemberNames = findViewById(R.id.etMemberNames);
        this.etMemberPhone = findViewById(R.id.etMemberPhone);
        this.btnSaveMember = findViewById(R.id.btnSaveMember);
        this.btnClear = findViewById(R.id.btnClear);
        this.btnContinueExpense = findViewById(R.id.btnContinueExpense);
        this.btnClaimMember = findViewById(R.id.btnClaimMember);
        this.tvEmptyMembers = findViewById(R.id.tvEmptyMembers);
        this.rvMembers = findViewById(R.id.rvMembers);

        this.userRepository = getServiceLocator().getUserRepository();
        this.memberAdapter = new MemberAdapter(this, this.userRepository.getCurrentUserId());
        this.rvMembers.setLayoutManager(new LinearLayoutManager(this));
        this.rvMembers.setAdapter(this.memberAdapter);
    }
}
