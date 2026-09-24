package ue.edu.co.splitbill.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

import static org.junit.Assert.assertEquals;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.ui.group.GroupDetailActivity;
import ue.edu.co.splitbill.ui.group.GroupFormActivity;
import ue.edu.co.splitbill.ui.quick.QuickSplitActivity;

/** Varios grupos, liquidacion con transferencias minimas y cuenta rapida. */
@RunWith(AndroidJUnit4.class)
public class GroupAndSettlementFlowTest extends UiTestSupport {

    @Before
    public void givenGroup() {
        givenLoggedInWithGroup();
    }

    /** Un grupo nuevo empieza vacio; al volver al primero, sus gastos siguen ahi. */
    @Test
    public void aNewGroupStartsEmptyAndTheFirstOneKeepsItsExpenses() {
        givenExpense("Almuerzo", this.julian, 6_000_000L, this.julian, 3_000_000L, this.diomar, 3_000_000L);
        try (ActivityScenario<GroupDetailActivity> ignored = ActivityScenario.launch(GroupDetailActivity.class)) {
            onView(withId(R.id.tvTitle)).perform(click());
            onView(withId(R.id.btnNewGroup)).perform(click());
            onView(withId(R.id.etGroupName)).perform(replaceText("Viaje"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveGroup)).perform(click());

            //al guardarlo se abre el grupo nuevo, vacio
            onView(withId(R.id.tvTitle)).check(matches(withText("Viaje")));
            onView(withId(R.id.tvEmptyExpenses)).check(matches(isDisplayed()));

            onView(withId(R.id.tvTitle)).perform(click());
            onView(withText("Mi grupo")).perform(click());
            onView(withId(R.id.tvTitle)).check(matches(withText("Mi grupo")));
            onView(withText("Almuerzo")).check(matches(isDisplayed()));
        }
    }

    /**
     * Julian paga 90.000 entre los tres y Diomar le paga 30.000 a Sofia. Sin simplificar serian 3
     * transferencias; la liquidacion propone 1: Sofia le paga 60.000 a Julian.
     */
    @Test
    public void settlementProposesTheMinimumTransfers() {
        givenExpense("Mercado", this.julian, 9_000_000L,
                this.julian, 3_000_000L, this.diomar, 3_000_000L, this.sofia, 3_000_000L);
        givenExpense("Taxi de Sofia", this.diomar, 3_000_000L, this.sofia, 3_000_000L);
        try (ActivityScenario<GroupDetailActivity> ignored = ActivityScenario.launch(GroupDetailActivity.class)) {
            onView(withId(R.id.btnSettle)).perform(click());

            onView(withId(R.id.tvTransferSummary)).check(matches(withText("1 transferencia en lugar de 3")));
            onView(withId(R.id.rvTransfers)).check(matches(hasDescendant(withText(containsString("Sofia")))));
            onView(withId(R.id.rvTransfers)).check(matches(hasDescendant(
                    withText(Money.ofCents(6_000_000L).format()))));
        }
    }

    /**
     * "Marcar como pagado": Sofia le paga a Julian lo que le debe. El pago queda como un gasto PAYMENT
     * y, al recargar, todos quedan en cero.
     */
    @Test
    public void markingEverythingAsPaidLeavesTheGroupSettled() {
        givenExpense("Mercado", this.julian, 9_000_000L,
                this.julian, 3_000_000L, this.diomar, 3_000_000L, this.sofia, 3_000_000L);
        givenExpense("Taxi de Sofia", this.diomar, 3_000_000L, this.sofia, 3_000_000L);
        try (ActivityScenario<GroupDetailActivity> ignored = ActivityScenario.launch(GroupDetailActivity.class)) {
            onView(withId(R.id.btnSettle)).perform(click());
            onView(withId(R.id.btnMarkAllPaid)).perform(click());
            onView(withText("Registrar pago")).inRoot(isDialog()).perform(click());

            onView(withId(R.id.tvAllSettled)).check(matches(isDisplayed()));
            assertEquals(3, this.database.expenseDao().countAll());
            //el pago no suma al total gastado del grupo
            assertEquals(12_000_000L, this.database.expenseDao().sumActiveCents(this.group.getId()));
        }
    }

    /** Nuevo grupo con su gente escrita en el mismo formulario: todo se guarda junto al final. */
    @Test
    public void aNewGroupIsSavedWithTheMembersTypedInTheForm() {
        try (ActivityScenario<GroupFormActivity> ignored = ActivityScenario.launch(GroupFormActivity.class)) {
            onView(withId(R.id.etGroupName)).perform(replaceText("Apartamento"));
            onView(withId(R.id.etMemberNames)).perform(replaceText("Luis"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveMember)).perform(click());
            onView(withId(R.id.etMemberNames)).perform(replaceText("Maria"));
            onView(withId(R.id.etMemberPhone)).perform(replaceText("3209876543"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveMember)).perform(click());
            onView(withId(R.id.tvMemberCount)).check(matches(withText("3 integrantes")));

            onView(withId(R.id.btnSaveGroup)).perform(click());

            onView(withId(R.id.tvTitle)).check(matches(withText("Apartamento")));
            assertEquals(3, this.database.groupMemberDao().countActive(this.sessionManager.getCurrentGroupId()));
        }
    }

    /** 100.000 con 10 % de propina entre 4: 27.500 cada uno; se puede pasar al formulario de gastos. */
    @Test
    public void quickSplitDividesWithTipAndCanBecomeAnExpense() {
        try (ActivityScenario<QuickSplitActivity> ignored = ActivityScenario.launch(QuickSplitActivity.class)) {
            onView(withId(R.id.etQuickTotal)).perform(replaceText("100000"));
            onView(withId(R.id.btnCalculate)).perform(click());

            onView(withId(R.id.rvQuickShares)).check(matches(hasDescendant(
                    withText(Money.ofCents(2_750_000L).format()))));

            onView(withId(R.id.btnSaveAsExpense)).perform(click());
            onView(withId(R.id.etAmount)).check(matches(withText("110000")));
            onView(withId(R.id.etDescription)).check(matches(withText("Cuenta rápida")));
        }
    }
}
