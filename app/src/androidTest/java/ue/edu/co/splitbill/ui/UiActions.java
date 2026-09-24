package ue.edu.co.splitbill.ui;

import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;

import android.view.View;
import android.widget.EditText;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import org.hamcrest.Matcher;

/**
 * Acciones de Espresso sobre una vista dentro de una fila de un RecyclerView (por ejemplo, el campo
 * de porcentaje de un participante o la papelera de un gasto).
 */
public final class UiActions {

    private UiActions() {
        //impide crear objetos de esta clase
    }

    public static ViewAction clickChild(final int childId) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(View.class);
            }

            @Override
            public String getDescription() {
                return "tocar la vista hija " + childId;
            }

            @Override
            public void perform(UiController uiController, View view) {
                view.findViewById(childId).performClick();
                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    public static ViewAction setTextInChild(final int childId, final String text) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(View.class);
            }

            @Override
            public String getDescription() {
                return "escribir " + text + " en la vista hija " + childId;
            }

            @Override
            public void perform(UiController uiController, View view) {
                ((EditText) view.findViewById(childId)).setText(text);
                uiController.loopMainThreadUntilIdle();
            }
        };
    }
}
