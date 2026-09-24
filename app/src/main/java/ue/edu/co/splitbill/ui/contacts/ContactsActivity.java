package ue.edu.co.splitbill.ui.contacts;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.model.Contact;
import ue.edu.co.splitbill.model.ContactRepository;
import ue.edu.co.splitbill.model.UserRepository;
import ue.edu.co.splitbill.permission.PermissionManager;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.SimpleTextWatcher;
import ue.edu.co.splitbill.ui.adapter.ContactAdapter;

/**
 * Elegir integrantes de la agenda del celular: se marcan varios y se agregan de una vez, con su
 * nombre y telefono, sin escribirlos.
 *
 * Integrantes solo abre esta pantalla con el permiso de contactos ya concedido (lo pide con
 * PermissionManager); aun asi se revisa aqui, porque el usuario puede quitarlo desde los ajustes.
 */
public class ContactsActivity extends BaseActivity implements ContactAdapter.OnSelectionChangedListener {

    /**
     * Grupo que todavia no existe (formulario de nuevo grupo): en vez de guardar, se devuelven los
     * contactos elegidos con setResult y el formulario los agrega a su lista.
     */
    public static final String EXTRA_RETURN_SELECTION = "extraReturnSelection";
    /** Nombres y telefonos de quienes ya estan en la lista del formulario, para marcarlos. */
    public static final String EXTRA_NAMES = "extraNames";
    public static final String EXTRA_PHONES = "extraPhones";

    private EditText etSearchContact;
    private TextView tvEmptyContacts;
    private RecyclerView rvContacts;
    private Button btnAddContacts;

    private ContactAdapter contactAdapter;
    private ContactRepository contactRepository;
    private UserRepository userRepository;
    private PermissionManager permissionManager;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_contacts;
    }

    @Override
    protected void initListeners() {
        this.btnAddContacts.setOnClickListener(this::addContactsDB);
        this.etSearchContact.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                contactAdapter.filter(s.toString());
                showEmptyState();
            }
        });

        //si ya marco contactos y no los agrego, se pregunta antes de salir
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (contactAdapter.getSelected().isEmpty()) {
                    finish();
                    return;
                }
                confirm(getString(R.string.dlgDiscardTitle), getString(R.string.dlgDiscardContactsMessage),
                        R.string.btnDiscard, ContactsActivity.this::finish);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }
        //si quitaron el permiso desde los ajustes (Android reinicia la app), se vuelve a Integrantes
        if (!this.permissionManager.hasPermissions(PermissionManager.CONTACTS)) {
            showToast(R.string.msgContactsRationale);
            finish();
            return;
        }
        listContacts();
    }

    /** Primero se leen los integrantes, para marcar los contactos que ya estan en el grupo. */
    private void listContacts() {
        if (getIntent().getBooleanExtra(EXTRA_RETURN_SELECTION, false)) {
            listContacts(usersFromIntent());
            return;
        }
        showLoading();
        this.userRepository.getActiveUsers(new UiCallback<List<User>>() {
            @Override
            protected void onData(List<User> members) {
                listContacts(members);
            }
        });
    }

    private void listContacts(List<User> members) {
        showLoading();
        this.contactRepository.getContacts(members, new UiCallback<List<Contact>>() {
            @Override
            protected void onData(List<Contact> contacts) {
                contactAdapter.setContacts(contacts);
                contactAdapter.filter(etSearchContact.getText().toString());
                showEmptyState();
            }
        });
    }

    /** Quienes ya estan en la lista del formulario de nuevo grupo, como personas para comparar. */
    private List<User> usersFromIntent() {
        List<User> users = new ArrayList<>();
        ArrayList<String> names = getIntent().getStringArrayListExtra(EXTRA_NAMES);
        ArrayList<String> phones = getIntent().getStringArrayListExtra(EXTRA_PHONES);
        if (names != null && phones != null) {
            for (int i = 0; i < names.size() && i < phones.size(); i++) {
                users.add(new User(names.get(i), null, phones.get(i)));
            }
        }
        return users;
    }

    private void showEmptyState() {
        this.tvEmptyContacts.setVisibility(this.contactAdapter.getVisibleCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onSelectionChanged(int selected) {
        this.btnAddContacts.setEnabled(selected > 0);
        this.btnAddContacts.setText(selected == 0 ? getString(R.string.btnAddContactsNone)
                : getResources().getQuantityString(R.plurals.btnAddContacts, selected, selected));
    }

    private void addContactsDB(View view) {
        if (getIntent().getBooleanExtra(EXTRA_RETURN_SELECTION, false)) {
            returnSelection();
            return;
        }
        List<User> users = new ArrayList<>();
        for (Contact contact : this.contactAdapter.getSelected()) {
            users.add(new User(contact.getNames(), null, contact.getPhone()));
        }
        showLoading();
        this.btnAddContacts.setEnabled(false);
        this.userRepository.insertUsers(users, new UiCallback<Integer>() {
            @Override
            protected void onData(Integer added) {
                showToast(getResources().getQuantityString(R.plurals.msgContactsAdded, added, added));
                //vuelve a Integrantes, que recarga la lista al reaparecer
                finish();
            }

            @Override
            public void onError(String message) {
                super.onError(message);
                btnAddContacts.setEnabled(true);
            }
        });
    }

    /** El grupo aun no existe: se devuelven nombres y telefonos y el formulario decide. */
    private void returnSelection() {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> phones = new ArrayList<>();
        for (Contact contact : this.contactAdapter.getSelected()) {
            names.add(contact.getNames());
            phones.add(contact.getPhone());
        }
        Intent data = new Intent();
        data.putStringArrayListExtra(EXTRA_NAMES, names);
        data.putStringArrayListExtra(EXTRA_PHONES, phones);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    protected void initObjects() {
        this.etSearchContact = findViewById(R.id.etSearchContact);
        this.tvEmptyContacts = findViewById(R.id.tvEmptyContacts);
        this.rvContacts = findViewById(R.id.rvContacts);
        this.btnAddContacts = findViewById(R.id.btnAddContacts);

        this.permissionManager = new PermissionManager(this);
        this.contactRepository = getServiceLocator().getContactRepository();
        this.userRepository = getServiceLocator().getUserRepository();
        this.contactAdapter = new ContactAdapter(this);
        this.rvContacts.setLayoutManager(new LinearLayoutManager(this));
        this.rvContacts.setAdapter(this.contactAdapter);
    }
}
