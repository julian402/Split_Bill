package ue.edu.co.splitbill.ui.group;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.model.UserRepository;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.adapter.MemberAdapter;

/**
 * Pantalla de integrantes del grupo: registrar, listar y dar de baja.
 *
 * Es el formulario mas sencillo de la aplicacion y sirve para verificar que toda la cadena funciona:
 * la pantalla llama al repositorio, el repositorio trabaja en otro hilo, Room escribe en SQLite y la
 * respuesta regresa al hilo principal para actualizar la lista.
 */
public class MembersActivity extends BaseActivity implements MemberAdapter.OnMemberDeleteListener {

    private EditText etMemberNames;
    private EditText etMemberPhone;
    private Button btnSaveMember;
    private Button btnClear;
    private TextView tvEmptyMembers;
    private RecyclerView rvMembers;

    private MemberAdapter memberAdapter;
    private UserRepository userRepository;
    private User user;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_members;
    }

    @Override
    protected void initListeners() {
        this.btnSaveMember.setOnClickListener(this::addMemberDB);
        this.btnClear.setOnClickListener(this::clearFieldsDB);
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
            }
        });
    }

    @Override
    public void onMemberDelete(User user) {
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
        this.tvEmptyMembers = findViewById(R.id.tvEmptyMembers);
        this.rvMembers = findViewById(R.id.rvMembers);

        this.userRepository = getServiceLocator().getUserRepository();
        this.memberAdapter = new MemberAdapter(this);
        this.rvMembers.setLayoutManager(new LinearLayoutManager(this));
        this.rvMembers.setAdapter(this.memberAdapter);
    }
}
