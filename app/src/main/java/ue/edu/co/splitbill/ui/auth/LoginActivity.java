package ue.edu.co.splitbill.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.model.SessionRepository;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.group.MainActivity;

/**
 * Primera pantalla de la aplicacion. Si ya hay una sesion abierta, pasa directo a MainActivity.
 */
public class LoginActivity extends BaseActivity {

    /** Lo manda BaseActivity cuando el servidor rechazo el token, para explicarle al usuario por que esta aqui. */
    public static final String EXTRA_SESSION_EXPIRED = "ue.edu.co.splitbill.SESSION_EXPIRED";

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnGoToRegister;

    private SessionRepository sessionRepository;
    private String email;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.sessionRepository.isLoggedIn()) {
            openMain();
            return;
        }
        if (getIntent().getBooleanExtra(EXTRA_SESSION_EXPIRED, false)) {
            showToast(R.string.msgSessionExpired);
        }
    }

    @Override
    protected boolean requiresSession() {
        return false;
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_login;
    }

    @Override
    protected void initListeners() {
        this.btnLogin.setOnClickListener(this::loginAPI);
        this.btnGoToRegister.setOnClickListener(this::openRegister);
    }

    private void loginAPI(View view) {
        getData();
        showLoading();
        this.btnLogin.setEnabled(false);
        this.sessionRepository.login(this.email, this.password, new UiCallback<String>() {
            @Override
            protected void onData(String names) {
                showToast(getString(R.string.msgWelcome, names));
                clearFields();
                openMain();
            }

            @Override
            public void onError(String message) {
                super.onError(message);
                btnLogin.setEnabled(true);
            }
        });
    }

    private void getData() {
        this.email = this.etEmail.getText().toString().trim();
        this.password = this.etPassword.getText().toString();
    }

    private void clearFields() {
        this.etEmail.setText("");
        this.etPassword.setText("");
    }

    private void openRegister(View view) {
        startActivity(new Intent(this, RegisterActivity.class));
    }

    /** Se limpia la pila para que "atras" desde la pantalla principal cierre la app y no vuelva al login. */
    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void initObjects() {
        this.etEmail = findViewById(R.id.etEmail);
        this.etPassword = findViewById(R.id.etPassword);
        this.btnLogin = findViewById(R.id.btnLogin);
        this.btnGoToRegister = findViewById(R.id.btnGoToRegister);

        this.sessionRepository = getServiceLocator().getSessionRepository();
    }
}
