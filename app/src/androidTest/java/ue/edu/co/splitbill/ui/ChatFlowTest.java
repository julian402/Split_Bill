package ue.edu.co.splitbill.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.entity.Message;
import ue.edu.co.splitbill.ui.group.GroupDetailActivity;

/**
 * Chat del grupo. El servidor falso no responde a los mensajes, asi que lo escrito queda "Enviando…":
 * es justo el caso sin conexion.
 */
@RunWith(AndroidJUnit4.class)
public class ChatFlowTest extends UiTestSupport {

    @Before
    public void givenGroup() {
        givenLoggedInWithGroup();
    }

    @Test
    public void aMessageIsSavedAndShownEvenWithoutConnection() {
        try (ActivityScenario<GroupDetailActivity> ignored = ActivityScenario.launch(GroupDetailActivity.class)) {
            onView(withId(R.id.cardChat)).perform(click());
            onView(withId(R.id.tvEmptyChat)).check(matches(isDisplayed()));

            onView(withId(R.id.etMessage)).perform(replaceText("Yo llevo el carbón"), closeSoftKeyboard());
            onView(withId(R.id.btnSend)).perform(click());

            onView(withId(R.id.rvMessages)).check(matches(hasDescendant(withText("Yo llevo el carbón"))));
            onView(withId(R.id.rvMessages)).check(matches(hasDescendant(withText(containsString("Enviando")))));
            onView(withId(R.id.etMessage)).check(matches(withText("")));
            List<Message> saved = this.database.messageDao().findByGroup(this.group.getId());
            assertEquals(1, saved.size());
            assertTrue(saved.get(0).isPending());

            //de vuelta en el grupo, la tarjeta muestra el ultimo mensaje
            pressBack();
            onView(withId(R.id.tvLastMessage)).check(matches(withText("Tú: Yo llevo el carbón")));
        }
    }
}
