package ue.edu.co.splitbill.ui.auth;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.model.SessionRepository;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.group.MainActivity;

/**
 * Crear una cuenta. Al registrarse el servidor devuelve el token, asi que la persona queda con la
 * sesion iniciada sin tener que escribir de nuevo su email y contrasena.
 */
public class RegisterActivity extends BaseActivity {

    private EditText etNames;
    private EditText etEmail;
    private EditText etPhone;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Button btnRegister;

    private SessionRepository sessionRepository;
    private String names;
    private String email;
    private String phone;
    private String password;
    private String confirmPassword;

    @Override
    protected boolean requiresSession() {
        return false;
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_register;
    }

    @Override
    protected void initListeners() {
        this.btnRegister.setOnClickListener(this::registerAPI);
    }

    private void registerAPI(View view) {
        getData();
        //esta comparacion es solo de la pantalla: el servidor nunca recibe la confirmacion
        if (!this.password.equals(this.confirmPassword)) {
            showToast(R.string.msgPasswordsDontMatch);
            return;
        }
        showLoading();
        this.btnRegister.setEnabled(false);
        this.sessionRepository.register(this.names, this.email, this.phone, this.password,
                new UiCallback<String>() {
                    @Override
                    protected void onData(String registeredNames) {
                        showToast(getString(R.string.msgWelcome, registeredNames));
                        clearFields();
                        openMain();
                    }

                    @Override
                    public void onError(String message) {
                        super.onError(message);
                        btnRegister.setEnabled(true);
                    }
                });
    }

    private void getData() {
        this.names = this.etNames.getText().toString().trim();
        this.email = this.etEmail.getText().toString().trim();
        this.phone = this.etPhone.getText().toString().trim();
        this.password = this.etPassword.getText().toString();
        this.confirmPassword = this.etConfirmPassword.getText().toString();
    }

    private void clearFields() {
        this.etNames.setText("");
        this.etEmail.setText("");
        this.etPhone.setText("");
        this.etPassword.setText("");
        this.etConfirmPassword.setText("");
    }

    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void initObjects() {
        this.etNames = findViewById(R.id.etNames);
        this.etEmail = findViewById(R.id.etEmail);
        this.etPhone = findViewById(R.id.etPhone);
        this.etPassword = findViewById(R.id.etPassword);
        this.etConfirmPassword = findViewById(R.id.etConfirmPassword);
        this.btnRegister = findViewById(R.id.btnRegister);

        this.sessionRepository = getServiceLocator().getSessionRepository();
    }
}
