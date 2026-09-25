package ue.edu.co.splitbill.ui;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.hasToString;
import static org.junit.Assert.assertEquals;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.ui.group.GroupDetailActivity;

/**
 * Registrar, ver, editar y borrar gastos, de punta a punta por la interfaz.
 *
 * Los mensajes de error salen en un Toast, que Espresso no puede leer en las versiones nuevas de
 * Android; por eso se verifica el efecto: el formulario sigue abierto y no se guardo nada.
 */
@RunWith(AndroidJUnit4.class)
public class ExpenseFlowTest extends UiTestSupport {

    @Before
    public void givenGroup() {
        givenLoggedInWithGroup();
    }

    @Test
    public void anExpenseWithoutAmountIsNotSaved() {
        try (ActivityScenario<GroupDetailActivity> ignored = ActivityScenario.launch(GroupDetailActivity.class)) {
            onView(withId(R.id.btnNavAdd)).perform(click());
            onView(withId(R.id.btnMenuExpense)).perform(click());
            onView(withId(R.id.etDescription)).perform(replaceText("Cena"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveExpense)).perform(click());

            onView(withId(R.id.btnSaveExpense)).check(matches(isDisplayed()));
            assertEquals(0, this.database.expenseDao().countAll());
        }
    }

    @Test
    public void percentagesThatDoNotAddUpTo100AreNotSaved() {
        try (ActivityScenario<GroupDetailActivity> ignored = ActivityScenario.launch(GroupDetailActivity.class)) {
            onView(withId(R.id.btnNavAdd)).perform(click());
            onView(withId(R.id.btnMenuExpense)).perform(click());
            onView(withId(R.id.etDescription)).perform(replaceText("Cena"));
            onView(withId(R.id.etAmount)).perform(replaceText("90000"), closeSoftKeyboard());
            onView(withId(R.id.spSplitType)).perform(click());
            onData(hasToString("Porcentajes")).perform(click());
            //Diomar, Julian y Sofia (por nombre): 50 + 30 + 10 = 90 %
            onView(withId(R.id.rvParticipants)).perform(actionOnItemAtPosition(0,
                    UiActions.setTextInChild(R.id.etParticipantValue, "50")));
            onView(withId(R.id.rvParticipants)).perform(actionOnItemAtPosition(1,
                    UiActions.setTextInChild(R.id.etParticipantValue, "30")));
            onView(withId(R.id.rvParticipants)).perform(actionOnItemAtPosition(2,
                    UiActions.setTextInChild(R.id.etParticipantValue, "10")));
            onView(withId(R.id.btnSaveExpense)).perform(click());

            onView(withId(R.id.btnSaveExpense)).check(matches(isDisplayed()));
            assertEquals(0, this.database.expenseDao().countAll());
        }
    }

    @Test
    public void aValidExpenseShowsUpInTheListAndTheTotal() {
        try (ActivityScenario<GroupDetailActivity> ignored = ActivityScenario.launch(GroupDetailActivity.class)) {
            onView(withId(R.id.btnNavAdd)).perform(click());
            onView(withId(R.id.btnMenuExpense)).perform(click());
            onView(withId(R.id.etDescription)).perform(replaceText("Cena"));
            onView(withId(R.id.etAmount)).perform(replaceText("90000"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveExpense)).perform(click());

            //de vuelta en el grupo
            onView(withText("Cena")).check(matches(isDisplayed()));
            onView(withId(R.id.tvTotal)).check(matches(withText(Money.ofCents(9_000_000L).format())));
            assertEquals(1, this.database.expenseDao().countAll());
        }
    }

    @Test
    public void anExpenseIsEditedFromItsDetail() {
        givenExpense("Almuerzo", this.julian, 6_000_000L, this.julian, 3_000_000L, this.diomar, 3_000_000L);
        try (ActivityScenario<GroupDetailActivity> ignored = ActivityScenario.launch(GroupDetailActivity.class)) {
            onView(withId(R.id.rvExpenses)).perform(actionOnItemAtPosition(0, click()));
            onView(withId(R.id.tvDetailDescription)).check(matches(withText("Almuerzo")));

            onView(withId(R.id.btnEditExpense)).perform(click());
            onView(withId(R.id.etDescription)).perform(replaceText("Almuerzo corregido"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveExpense)).perform(click());

            //al guardar vuelve al detalle, ya con el cambio
            onView(withId(R.id.tvDetailDescription)).check(matches(withText("Almuerzo corregido")));
            onView(withId(R.id.tvDetailAmount)).check(matches(withText(Money.ofCents(6_000_000L).format())));
        }
    }

    /** Desde el rediseno se borra desde el detalle del gasto, siempre con confirmacion. */
    @Test
    public void deletingAsksFirstAndCancelKeepsTheExpense() {
        givenExpense("Almuerzo", this.julian, 6_000_000L, this.julian, 3_000_000L, this.diomar, 3_000_000L);
        try (ActivityScenario<GroupDetailActivity> ignored = ActivityScenario.launch(GroupDetailActivity.class)) {
            onView(withId(R.id.rvExpenses)).perform(actionOnItemAtPosition(0, click()));
            onView(withId(R.id.btnDeleteExpense)).perform(scrollTo(), click());
            onView(withText("Cancelar")).inRoot(isDialog()).perform(click());
            onView(withId(R.id.tvDetailDescription)).check(matches(withText("Almuerzo")));

            onView(withId(R.id.btnDeleteExpense)).perform(scrollTo(), click());
            onView(withText("Eliminar")).inRoot(isDialog()).perform(click());
            onView(withText("Almuerzo")).check(doesNotExist());
            onView(withId(R.id.tvEmptyExpenses)).check(matches(isDisplayed()));
        }
    }
}
