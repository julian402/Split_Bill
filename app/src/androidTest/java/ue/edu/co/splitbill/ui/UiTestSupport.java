package ue.edu.co.splitbill.ui;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

import ue.edu.co.splitbill.SplitBillApplication;
import ue.edu.co.splitbill.di.ServiceLocator;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.domain.SplitType;
import ue.edu.co.splitbill.entity.Expense;
import ue.edu.co.splitbill.entity.ExpenseShare;
import ue.edu.co.splitbill.entity.Group;
import ue.edu.co.splitbill.entity.GroupMember;
import ue.edu.co.splitbill.entity.SyncStatus;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.manager.SplitBillDatabase;
import ue.edu.co.splitbill.session.SessionManager;

/**
 * Base de las pruebas de interfaz: deja la app limpia antes de cada prueba y trae atajos para
 * preparar datos (una sesion iniciada, un grupo con integrantes, gastos).
 *
 * Los datos se escriben directo en la base de datos en memoria de TestSplitBillApplication: lo que
 * se prueba es la pantalla, no el registro de los datos de partida.
 */
public abstract class UiTestSupport {

    protected ServiceLocator locator;
    protected SplitBillDatabase database;
    protected SessionManager sessionManager;

    protected Group group;
    protected User julian;
    protected User diomar;
    protected User sofia;

    @Before
    public void resetApp() {
        SplitBillApplication app = ApplicationProvider.getApplicationContext();
        this.locator = app.getServiceLocator();
        this.database = this.locator.getDatabase();
        this.sessionManager = this.locator.getSessionManager();
        this.sessionManager.clear();
        this.database.clearAllTables();
    }

    @After
    public void cleanUp() {
        this.sessionManager.clear();
        this.database.clearAllTables();
    }

    /** Sesion de Julian en el grupo "Mi grupo" con Diomar y Sofia, sin gastos. */
    protected void givenLoggedInWithGroup() {
        this.group = new Group("Mi grupo", "COP");
        this.database.groupDao().insert(this.group);
        this.julian = new User(FakeBackend.USER_NAMES, "julian@test.co", null);
        this.julian.setId(FakeBackend.USER_ID);
        this.diomar = new User("Diomar", null, null);
        this.sofia = new User("Sofia", null, null);
        for (User user : new User[]{this.julian, this.diomar, this.sofia}) {
            this.database.userDao().insert(user);
            this.database.groupMemberDao().upsert(new GroupMember(this.group.getId(), user.getId(), SyncStatus.SYNCED));
        }
        this.sessionManager.startSession("token-de-prueba", this.julian.getId(), this.julian.getNames(),
                this.julian.getEmail());
        this.sessionManager.setCurrentGroupId(this.group.getId());
    }

    /**
     * Guarda un gasto ya repartido.
     *
     * @param shares pares persona-centavos, por ejemplo (julian, 3_000_000L, diomar, 3_000_000L)
     */
    protected Expense givenExpense(String description, User payer, long cents, Object... shares) {
        Expense expense = new Expense(this.group.getId(), payer.getId(), description, Money.ofCents(cents),
                SplitType.EXACT);
        this.database.expenseDao().insert(expense);
        List<ExpenseShare> rows = new ArrayList<>();
        for (int i = 0; i < shares.length; i += 2) {
            ExpenseShare share = new ExpenseShare();
            share.setExpenseId(expense.getId());
            share.setUserId(((User) shares[i]).getId());
            share.setAmountCents((Long) shares[i + 1]);
            rows.add(share);
        }
        this.database.expenseShareDao().insertAll(rows);
        return expense;
    }
}
