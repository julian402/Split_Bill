package ue.edu.co.splitbill.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import ue.edu.co.splitbill.R;

/**
 * Menu del boton + de la barra inferior.
 *
 * Al tocar el + se oscurece la pantalla, el + gira hasta quedar como una X y las dos opciones
 * ("Gasto" y "Cuenta rapida") salen desde el boton hacia arriba. Tocar el fondo, la X o "atras" lo
 * cierra. La vista se agrega encima de la pantalla la primera vez que se abre, asi ningun layout
 * tiene que incluirla.
 */
public class AddMenu {

    private static final long OPEN_DURATION = 260L;
    private static final long CLOSE_DURATION = 160L;
    private static final long STAGGER = 60L;
    private static final float CLOSED_SCALE = 0.6f;
    private static final float OPEN_ROTATION = 135f;
    /** Distancia (en dp) desde la que salen las opciones: parten de encima del +. */
    private static final int OPTION_OFFSET_DP = 56;
    private static final int OPTIONS_GAP_DP = 12;

    private final AppCompatActivity activity;
    private final View addButton;
    private final View.OnClickListener onExpense;
    private final View.OnClickListener onQuickSplit;
    private final OnBackPressedCallback backCallback;

    private View menu;
    private View scrim;
    private View options;
    private View btnExpense;
    private View btnQuickSplit;
    private View closeRing;
    private View btnClose;

    /**
     * @param addButton    el anillo blanco del + en la barra, para poner la X justo encima
     * @param onExpense    que hacer al escoger "Gasto"
     * @param onQuickSplit que hacer al escoger "Cuenta rapida"
     */
    public AddMenu(AppCompatActivity activity, View addButton,
                   View.OnClickListener onExpense, View.OnClickListener onQuickSplit) {
        this.activity = activity;
        this.addButton = addButton;
        this.onExpense = onExpense;
        this.onQuickSplit = onQuickSplit;
        this.backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                close(true);
            }
        };
        activity.getOnBackPressedDispatcher().addCallback(activity, this.backCallback);
    }

    public boolean isOpen() {
        return this.menu != null && this.menu.getVisibility() == View.VISIBLE;
    }

    public void open() {
        if (isOpen()) {
            return;
        }
        if (this.menu == null) {
            initObjects();
            initListeners();
        }
        placeOverAddButton();
        this.menu.setVisibility(View.VISIBLE);
        this.backCallback.setEnabled(true);

        this.scrim.setAlpha(0f);
        this.scrim.animate().alpha(1f).setDuration(OPEN_DURATION).start();
        this.btnClose.setRotation(0f);
        this.btnClose.animate().rotation(OPEN_ROTATION).setDuration(OPEN_DURATION)
                .setInterpolator(new DecelerateInterpolator()).start();
        //cada opcion sale desde el + hacia su lugar, una detras de la otra
        showOption(this.btnExpense, 1, 0L);
        showOption(this.btnQuickSplit, -1, STAGGER);
    }

    /**
     * @param animated false al escoger una opcion: la pantalla nueva tapa el menu, y al volver ya
     *                 debe estar cerrado
     */
    public void close(boolean animated) {
        if (!isOpen()) {
            return;
        }
        this.backCallback.setEnabled(false);
        if (!animated) {
            this.menu.setVisibility(View.GONE);
            return;
        }
        this.btnClose.animate().rotation(0f).setDuration(CLOSE_DURATION).start();
        hideOption(this.btnExpense, 1);
        hideOption(this.btnQuickSplit, -1);
        //al terminar de desvanecerse se oculta; si se volvio a abrir mientras tanto, se queda abierto
        this.scrim.animate().alpha(0f).setDuration(CLOSE_DURATION).withEndAction(() -> {
            if (!this.backCallback.isEnabled()) {
                this.menu.setVisibility(View.GONE);
            }
        }).start();
    }

    /**
     * @param side 1 si la opcion esta a la izquierda del + (sale hacia la izquierda), -1 si esta a la
     *             derecha
     */
    private void showOption(View option, int side, long delay) {
        float offset = dp(OPTION_OFFSET_DP);
        option.setAlpha(0f);
        option.setScaleX(CLOSED_SCALE);
        option.setScaleY(CLOSED_SCALE);
        option.setTranslationX(side * offset);
        option.setTranslationY(offset);
        option.animate().alpha(1f).scaleX(1f).scaleY(1f).translationX(0f).translationY(0f)
                .setStartDelay(delay).setDuration(OPEN_DURATION)
                .setInterpolator(new OvershootInterpolator(1.2f)).start();
    }

    private void hideOption(View option, int side) {
        float offset = dp(OPTION_OFFSET_DP);
        option.animate().alpha(0f).scaleX(CLOSED_SCALE).scaleY(CLOSED_SCALE)
                .translationX(side * offset).translationY(offset)
                .setStartDelay(0L).setDuration(CLOSE_DURATION)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    /** Pone la X exactamente sobre el + de la barra y las opciones justo encima de el. */
    private void placeOverAddButton() {
        int[] button = new int[2];
        int[] container = new int[2];
        this.addButton.getLocationInWindow(button);
        ((View) this.menu.getParent()).getLocationInWindow(container);
        int top = button[1] - container[1];
        this.closeRing.setX(button[0] - container[0]);
        this.closeRing.setY(top);

        int parentHeight = ((View) this.menu.getParent()).getHeight();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) this.options.getLayoutParams();
        params.bottomMargin = parentHeight - top + (int) dp(OPTIONS_GAP_DP);
        this.options.setLayoutParams(params);
    }

    private void choose(View option, View.OnClickListener action) {
        close(false);
        action.onClick(option);
    }

    private void chooseExpense(View view) {
        choose(view, this.onExpense);
    }

    private void chooseQuickSplit(View view) {
        choose(view, this.onQuickSplit);
    }

    private void closeAnimated(View view) {
        close(true);
    }

    private float dp(int value) {
        return value * this.activity.getResources().getDisplayMetrics().density;
    }

    private void initListeners() {
        this.scrim.setOnClickListener(this::closeAnimated);
        this.btnClose.setOnClickListener(this::closeAnimated);
        this.btnExpense.setOnClickListener(this::chooseExpense);
        this.btnQuickSplit.setOnClickListener(this::chooseQuickSplit);
    }

    /** Agrega el menu encima de todo el contenido de la pantalla. */
    private void initObjects() {
        ViewGroup content = this.activity.findViewById(android.R.id.content);
        this.menu = LayoutInflater.from(this.activity).inflate(R.layout.view_add_menu, content, false);
        content.addView(this.menu);
        this.scrim = this.menu.findViewById(R.id.addMenuScrim);
        this.options = this.menu.findViewById(R.id.addMenuOptions);
        this.btnExpense = this.menu.findViewById(R.id.btnMenuExpense);
        this.btnQuickSplit = this.menu.findViewById(R.id.btnMenuQuickSplit);
        this.closeRing = this.menu.findViewById(R.id.addMenuClose);
        this.btnClose = this.menu.findViewById(R.id.btnAddMenuClose);
    }
}
