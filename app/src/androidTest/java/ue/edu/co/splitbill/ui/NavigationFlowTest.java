package ue.edu.co.splitbill.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.ui.home.HomeActivity;

/**
 * La barra inferior: las cuatro pestanas llevan a su pantalla y el inicio resume los grupos.
 */
@RunWith(AndroidJUnit4.class)
public class NavigationFlowTest extends UiTestSupport {

    @Before
    public void givenGroup() {
        givenLoggedInWithGroup();
    }

    @Test
    public void theBottomBarOpensEachTab() {
        givenExpense("Almuerzo", this.julian, 6_000_000L, this.julian, 3_000_000L, this.diomar, 3_000_000L);
        try (ActivityScenario<HomeActivity> ignored = ActivityScenario.launch(HomeActivity.class)) {
            //el inicio muestra el grupo y el gasto
            onView(withText("Mi grupo")).check(matches(isDisplayed()));
            onView(withId(R.id.tvHomeCounts)).check(matches(withText("1 grupo · 1 gasto")));

            onView(withId(R.id.navActivity)).perform(click());
            onView(withId(R.id.rvActivity)).check(matches(isDisplayed()));
            onView(withText("Almuerzo")).check(matches(isDisplayed()));

            onView(withId(R.id.navProfile)).perform(click());
            onView(withId(R.id.tvProfileName)).check(matches(withText(FakeBackend.USER_NAMES)));

            onView(withId(R.id.navGroups)).perform(click());
            onView(withId(R.id.btnNewGroup)).check(matches(isDisplayed()));

            onView(withId(R.id.navHome)).perform(click());
            onView(withId(R.id.tvGreeting)).check(matches(isDisplayed()));
        }
    }
}
