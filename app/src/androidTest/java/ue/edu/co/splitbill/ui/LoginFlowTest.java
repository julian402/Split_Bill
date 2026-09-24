package ue.edu.co.splitbill.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.ui.auth.LoginActivity;

/** Inicio de sesion contra el servidor falso. */
@RunWith(AndroidJUnit4.class)
public class LoginFlowTest extends UiTestSupport {

    @Test
    public void wrongPasswordKeepsTheUserOnTheLoginScreen() {
        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            login("julian@test.co", "otra-clave-123");

            onView(withId(R.id.btnLogin)).check(matches(isDisplayed()));
            assertNull(this.sessionManager.getToken());
        }
    }

    /**
     * Con la clave correcta entra al inicio. Como el servidor no tiene grupos para esta persona, la
     * app le crea "Mi grupo" y la deja como integrante; el grupo aparece en "Tus grupos".
     */
    @Test
    public void correctPasswordOpensTheGroup() {
        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            login("julian@test.co", FakeBackend.PASSWORD);

            onView(withId(R.id.tvGreeting)).check(matches(withText("Hola, " + FakeBackend.USER_NAMES)));
            onView(withText("Mi grupo")).check(matches(isDisplayed()));
            assertNotNull(this.sessionManager.getToken());
            assertEquals(1, this.database.groupMemberDao().countActive(this.sessionManager.getCurrentGroupId()));
        }
    }

    private void login(String email, String password) {
        onView(withId(R.id.etEmail)).perform(replaceText(email));
        onView(withId(R.id.etPassword)).perform(replaceText(password), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).perform(click());
    }
}
