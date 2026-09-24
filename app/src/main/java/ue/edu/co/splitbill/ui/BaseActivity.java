package ue.edu.co.splitbill.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.SplitBillApplication;
import ue.edu.co.splitbill.di.ServiceLocator;
import ue.edu.co.splitbill.model.DataCallback;
import ue.edu.co.splitbill.ui.auth.LoginActivity;

/**
 * Base de todas las pantallas de la aplicacion.
 *
 * Recoge lo que se repetia igual en cada Activity: habilitar el modo de pantalla completa, aplicar
 * los margenes de la barra de estado y la barra de navegacion, mostrar mensajes y pedirle al
 * ServiceLocator las dependencias.
 *
 * Es el patron metodo plantilla: onCreate define el orden de arranque de cualquier pantalla
 * (inflar el layout, aplicar margenes, enlazar vistas, enlazar listeners) y cada hija solo llena
 * los huecos. Asi ninguna pantalla vuelve a copiar el bloque de insets.
 *
 * Desde la entrega 3 tambien protege las pantallas: si no hay sesion (o el token vencio), manda al
 * login. Las pantallas de login y registro lo desactivan sobrescribiendo requiresSession().
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (requiresSession() && !hasSession()) {
            //finish() dentro de onCreate hace que Android no llegue a onResume de esta pantalla
            goToLogin(false);
            return;
        }
        EdgeToEdge.enable(this);
        setContentView(getLayoutResourceId());
        applyWindowInsets();
        initToolbar();
        initObjects();
        initListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //el token pudo vencer mientras la pantalla estaba abierta (AuthInterceptor lo detecta)
        if (requiresSession() && !hasSession()) {
            goToLogin(true);
        }
    }

    /** Casi todas las pantallas necesitan sesion; login y registro responden false. */
    protected boolean requiresSession() {
        return true;
    }

    private boolean hasSession() {
        return getServiceLocator().getSessionManager().isLoggedIn();
    }

    /** Abre el login y cierra todas las pantallas anteriores, para que "atras" no regrese aqui. */
    protected void goToLogin(boolean sessionExpired) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(LoginActivity.EXTRA_SESSION_EXPIRED, sessionExpired);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** Layout que infla la pantalla. Su vista raiz debe llevar android:id="@+id/main". */
    protected abstract int getLayoutResourceId();

    /** Enlaza los atributos de la clase con las vistas del layout usando findViewById. */
    protected abstract void initObjects();

    /** Enlaza los botones con sus metodos. */
    protected abstract void initListeners();

    /** Evita que el contenido quede debajo de la barra de estado o de la de navegacion. */
    private void applyWindowInsets() {
        View root = findViewById(R.id.main);
        if (root == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /** Si la pantalla tiene barra superior, su flecha regresa a la pantalla anterior. */
    private void initToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(this::goBack);
        }
    }

    private void goBack(View view) {
        getOnBackPressedDispatcher().onBackPressed();
    }

    protected ServiceLocator getServiceLocator() {
        return ((SplitBillApplication) getApplication()).getServiceLocator();
    }

    protected void showToast(String message) {
        if (message == null || message.trim().isEmpty()) {
            message = getString(R.string.msgUnexpectedError);
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    protected void showToast(int messageResourceId) {
        Toast.makeText(this, getString(messageResourceId), Toast.LENGTH_LONG).show();
    }

    protected void showLoading() {
        setLoadingVisible(true);
    }

    protected void hideLoading() {
        setLoadingVisible(false);
    }

    private void setLoadingVisible(boolean visible) {
        View progressBar = findViewById(R.id.pbLoading);
        if (progressBar != null) {
            progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /** La pantalla sigue viva y se pueden tocar sus vistas. */
    protected boolean isAlive() {
        return !isFinishing() && !isDestroyed();
    }

    /**
     * Callback que ya sabe comportarse en una pantalla.
     *
     * Como los repositorios responden despues, puede ocurrir que el usuario gire el celular o salga
     * de la pantalla antes de que llegue la respuesta. Esta clase verifica que la Activity siga viva
     * antes de tocar las vistas y, si algo falla, muestra el mensaje sin que la pantalla lo repita.
     *
     * @param <T> tipo de dato que devuelve la operacion
     */
    protected abstract class UiCallback<T> implements DataCallback<T> {

        @Override
        public final void onSuccess(T data) {
            if (isAlive()) {
                hideLoading();
                onData(data);
            }
        }

        @Override
        public void onError(String message) {
            if (isAlive()) {
                hideLoading();
                showToast(message);
            }
        }

        /** Que hacer con el dato cuando llega y la pantalla sigue viva. */
        protected abstract void onData(T data);
    }
}
