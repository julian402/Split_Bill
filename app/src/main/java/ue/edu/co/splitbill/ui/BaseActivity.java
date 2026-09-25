package ue.edu.co.splitbill.ui;

import android.content.Intent;
import android.os.Bundle;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.SplitBillApplication;
import ue.edu.co.splitbill.di.ServiceLocator;
import ue.edu.co.splitbill.model.DataCallback;
import ue.edu.co.splitbill.ui.auth.LoginActivity;
import ue.edu.co.splitbill.ui.expense.AddExpenseActivity;
import ue.edu.co.splitbill.ui.feed.ActivityFeedActivity;
import ue.edu.co.splitbill.ui.group.GroupFormActivity;
import ue.edu.co.splitbill.ui.group.GroupsActivity;
import ue.edu.co.splitbill.ui.home.HomeActivity;
import ue.edu.co.splitbill.ui.profile.ProfileActivity;
import ue.edu.co.splitbill.ui.quick.QuickSplitActivity;

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
 *
 * Desde el rediseno maneja la barra inferior (Inicio, Grupos, +, Actividad, Perfil): la pantalla que
 * la incluye en su layout devuelve su pestana en getNavItem() y BaseActivity hace el resto. El + abre
 * el menu con "Gasto" y "Cuenta rapida" (AddMenu).
 */
public abstract class BaseActivity extends AppCompatActivity {

    /** Un gasto no se puede repartir si no hay al menos dos integrantes. */
    protected static final int MIN_MEMBERS = 2;

    /** Sin pestana marcada: la pantalla muestra la barra pero no es una de las cuatro principales. */
    protected static final int NAV_NONE = 0;

    /** Menu del boton +; se crea solo si la pantalla tiene barra inferior. */
    private AddMenu addMenu;

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
        initBottomNav();
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

    /**
     * Evita que el contenido quede debajo de la barra de estado o de la de navegacion. Si la pantalla
     * tiene barra inferior, el margen de abajo se lo lleva la barra, para que su fondo blanco llegue
     * hasta el borde del celular.
     */
    private void applyWindowInsets() {
        View root = findViewById(R.id.main);
        if (root == null) {
            return;
        }
        final View navBar = findViewById(R.id.navBar);
        final int navBarPadding = navBar == null ? 0 : navBar.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (navBar == null) {
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            } else {
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
                navBar.setPadding(navBar.getPaddingLeft(), navBar.getPaddingTop(), navBar.getPaddingRight(),
                        navBarPadding + systemBars.bottom);
            }
            return insets;
        });
    }

    /** Si el encabezado de la pantalla tiene boton de atras, regresa a la pantalla anterior. */
    private void initToolbar() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(this::goBack);
        }
    }

    /**
     * Pestana de la barra inferior que corresponde a esta pantalla: R.id.navHome, R.id.navGroups,
     * R.id.navActivity o R.id.navProfile. NAV_NONE si la barra no marca ninguna.
     */
    protected int getNavItem() {
        return NAV_NONE;
    }

    /** Enlaza las cuatro pestanas y el menu del boton + si el layout incluye view_bottom_nav. */
    private void initBottomNav() {
        if (findViewById(R.id.bottomNav) == null) {
            return;
        }
        bindNavItem(R.id.navHome, R.id.navHomeIcon, R.id.navHomeLabel, HomeActivity.class);
        bindNavItem(R.id.navGroups, R.id.navGroupsIcon, R.id.navGroupsLabel, GroupsActivity.class);
        bindNavItem(R.id.navActivity, R.id.navActivityIcon, R.id.navActivityLabel, ActivityFeedActivity.class);
        bindNavItem(R.id.navProfile, R.id.navProfileIcon, R.id.navProfileLabel, ProfileActivity.class);
        this.addMenu = new AddMenu(this, findViewById(R.id.navAddRing), this::openAddExpense, this::openQuickSplit);
        findViewById(R.id.btnNavAdd).setOnClickListener(this::openAddMenu);
    }

    private void openAddMenu(View view) {
        this.addMenu.open();
    }

    private void bindNavItem(int itemId, int iconId, int labelId, final Class<?> target) {
        final boolean selected = getNavItem() == itemId;
        ImageView icon = findViewById(iconId);
        TextView label = findViewById(labelId);
        int color = ContextCompat.getColor(this, selected ? R.color.colorPrimary : R.color.colorTextSecondary);
        icon.setImageTintList(ColorStateList.valueOf(color));
        icon.setBackgroundResource(selected ? R.drawable.bg_nav_selected : 0);
        label.setTextColor(color);
        label.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        View item = findViewById(itemId);
        item.setSelected(selected);
        //desde una pantalla interna (un grupo, por ejemplo) la pestana marcada lleva a su lista
        item.setOnClickListener(view -> {
            if (!getClass().equals(target)) {
                openTab(target);
            }
        });
    }

    /**
     * Cambia de pestana sin apilar pantallas: si la pestana ya estaba abierta, se vuelve a ella y se
     * cierran las que tenia encima (CLEAR_TOP); si no, se abre. Sin animacion, como una pestana.
     */
    protected void openTab(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    /**
     * "Gasto" en el menu del +: registra un gasto en el grupo actual. Antes se verifica que haya con quien
     * repartirlo; si no, se lleva a agregar integrantes y desde alli se sigue con el gasto.
     */
    protected void openAddExpense(View view) {
        getServiceLocator().getUserRepository().countActiveUsers(new UiCallback<Integer>() {
            @Override
            protected void onData(Integer data) {
                if (data < MIN_MEMBERS) {
                    showToast(R.string.msgNeedTwoMembers);
                    Intent intent = new Intent(BaseActivity.this, GroupFormActivity.class);
                    intent.putExtra(GroupFormActivity.EXTRA_CONTINUE_TO_EXPENSE, true);
                    startActivity(intent);
                    return;
                }
                startActivity(new Intent(BaseActivity.this, AddExpenseActivity.class));
            }
        });
    }

    /** "Cuenta rapida" en el menu del +: no necesita integrantes registrados, se abre siempre. */
    protected void openQuickSplit(View view) {
        startActivity(new Intent(this, QuickSplitActivity.class));
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

    /**
     * Pide confirmacion antes de una accion que no se puede deshacer desde la pantalla (borrar,
     * cerrar sesion). Si el usuario acepta, se ejecuta la accion; si cancela, no pasa nada.
     */
    protected void confirm(String title, String message, int positiveButtonResourceId, final Runnable action) {
        confirm(title, message, positiveButtonResourceId, R.string.btnCancel, action);
    }

    protected void confirm(String title, String message, int positiveButtonResourceId,
                           int negativeButtonResourceId, final Runnable action) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(negativeButtonResourceId, null)
                .setPositiveButton(positiveButtonResourceId, (dialog, which) -> action.run())
                .show();
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
