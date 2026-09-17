package ue.edu.co.splitbill.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.SplitBillApplication;
import ue.edu.co.splitbill.di.ServiceLocator;
import ue.edu.co.splitbill.model.DataCallback;

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
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(getLayoutResourceId());
        applyWindowInsets();
        initObjects();
        initListeners();
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
