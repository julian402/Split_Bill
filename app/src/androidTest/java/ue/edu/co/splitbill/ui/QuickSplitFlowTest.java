package ue.edu.co.splitbill.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.dao.QuickSplitListItem;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.entity.QuickSplitShare;
import ue.edu.co.splitbill.ui.quick.QuickSplitActivity;

/**
 * Cuenta rapida: calcular y luego guardarla en un grupo (con las mismas personas) o en cuentas
 * rapidas, sin grupo. "Mi grupo" tiene tres integrantes: Julian, Diomar y Sofia.
 */
@RunWith(AndroidJUnit4.class)
public class QuickSplitFlowTest extends UiTestSupport {

    @Before
    public void givenGroup() {
        givenLoggedInWithGroup();
    }

    /** 100.000 con 10 % de propina entre 4: 27.500 cada uno. Se guarda sin grupo, con su nombre. */
    @Test
    public void aQuickSplitIsSavedWithoutGroup() {
        try (ActivityScenario<QuickSplitActivity> ignored = ActivityScenario.launch(QuickSplitActivity.class)) {
            onView(withId(R.id.etQuickTotal)).perform(replaceText("100000"), closeSoftKeyboard());
            onView(withId(R.id.rvQuickShares)).perform(actionOnItemAtPosition(0,
                    UiActions.setTextInChild(R.id.etPersonName, "Ana")));
            onView(withId(R.id.btnCalculate)).perform(click());
            onView(withId(R.id.rvQuickShares)).check(matches(hasDescendant(
                    withText(Money.ofCents(2_750_000L).format()))));

            onView(withId(R.id.btnSaveQuickSplit)).perform(click());
            onView(withId(R.id.optionSaveQuickSplit)).perform(click());
            onView(withId(R.id.etQuickSplitName)).inRoot(isDialog()).perform(replaceText("Cena"));
            onView(withText("Guardar")).inRoot(isDialog()).perform(click());

            //queda en la lista de cuentas rapidas guardadas
            onView(withText("Cena")).check(matches(isDisplayed()));
            List<QuickSplitListItem> saved = this.database.quickSplitDao().findActive();
            assertEquals(1, saved.size());
            assertEquals(11_000_000L, saved.get(0).getTotalCents());
            List<QuickSplitShare> shares = this.database.quickSplitDao().findShares(saved.get(0).getQuickSplitId());
            assertEquals(4, shares.size());
            assertEquals("Ana", shares.get(0).getName());
            assertEquals("Persona 2", shares.get(1).getName());
        }
    }

    /** Entre 3 y en un grupo de 3: el formulario llega con el total y los tres marcados. */
    @Test
    public void aQuickSplitGoesToAGroupWithTheSamePeople() {
        try (ActivityScenario<QuickSplitActivity> ignored = ActivityScenario.launch(QuickSplitActivity.class)) {
            onView(withId(R.id.etQuickTotal)).perform(replaceText("90000"), closeSoftKeyboard());
            onView(withId(R.id.etTipPercentage)).perform(replaceText("0"), closeSoftKeyboard());
            onView(withId(R.id.btnMinus)).perform(click());
            onView(withId(R.id.btnCalculate)).perform(click());

            onView(withId(R.id.btnSaveQuickSplit)).perform(click());
            onView(withId(R.id.optionSaveInGroup)).perform(click());
            onView(withText("Mi grupo")).inRoot(isDialog()).perform(click());

            onView(withId(R.id.etAmount)).check(matches(withText("90000")));
            onView(withId(R.id.tvFromQuickSplit)).check(matches(isDisplayed()));
            onView(withId(R.id.tvParticipantCount)).check(matches(withText("3 participantes")));
            onView(withId(R.id.btnSaveExpense)).perform(click());

            onView(withId(R.id.tvTitle)).check(matches(withText("Mi grupo")));
            assertEquals(9_000_000L, this.database.expenseDao().sumActiveCents(this.group.getId()));
        }
    }

    /** Entre 4 y el unico grupo tiene 3 integrantes: no hay grupo donde quepa, no se abre el formulario. */
    @Test
    public void aGroupWithFewerMembersThanPeopleCannotBeChosen() {
        try (ActivityScenario<QuickSplitActivity> ignored = ActivityScenario.launch(QuickSplitActivity.class)) {
            onView(withId(R.id.etQuickTotal)).perform(replaceText("100000"), closeSoftKeyboard());
            onView(withId(R.id.btnCalculate)).perform(click());

            onView(withId(R.id.btnSaveQuickSplit)).perform(click());
            onView(withId(R.id.optionSaveInGroup)).perform(click());

            onView(withId(R.id.etAmount)).check(doesNotExist());
            onView(withId(R.id.btnSaveQuickSplit)).check(matches(isDisplayed()));
        }
    }

    /** Entre 2 en un grupo de 3: si se marcan los 3, no se guarda; con 2, si. */
    @Test
    public void theExpenseMustKeepTheNumberOfPeopleOfTheQuickSplit() {
        try (ActivityScenario<QuickSplitActivity> ignored = ActivityScenario.launch(QuickSplitActivity.class)) {
            onView(withId(R.id.etQuickTotal)).perform(replaceText("50000"), closeSoftKeyboard());
            onView(withId(R.id.etTipPercentage)).perform(replaceText("0"), closeSoftKeyboard());
            onView(withId(R.id.btnMinus)).perform(click());
            onView(withId(R.id.btnMinus)).perform(click());
            onView(withId(R.id.rvQuickShares)).perform(actionOnItemAtPosition(0,
                    UiActions.setTextInChild(R.id.etPersonName, "Diomar")));
            onView(withId(R.id.rvQuickShares)).perform(actionOnItemAtPosition(1,
                    UiActions.setTextInChild(R.id.etPersonName, "sofia")));
            onView(withId(R.id.btnCalculate)).perform(click());

            onView(withId(R.id.btnSaveQuickSplit)).perform(click());
            onView(withId(R.id.optionSaveInGroup)).perform(click());
            onView(withText("Mi grupo")).inRoot(isDialog()).perform(click());

            //se marcan solas las dos personas que se llaman igual (sin importar mayusculas)
            onView(withId(R.id.tvParticipantCount)).check(matches(withText("2 participantes")));
            onView(withId(R.id.tvSelectAll)).perform(click());
            onView(withId(R.id.tvParticipantCount)).check(matches(withText("3 participantes")));
            onView(withId(R.id.btnSaveExpense)).perform(click());
            onView(withId(R.id.btnSaveExpense)).check(matches(isDisplayed()));
            assertEquals(0, this.database.expenseDao().countAll());
        }
    }
}
