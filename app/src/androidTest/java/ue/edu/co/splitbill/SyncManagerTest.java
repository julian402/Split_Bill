package ue.edu.co.splitbill;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import ue.edu.co.splitbill.di.AppExecutors;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.domain.SplitType;
import ue.edu.co.splitbill.entity.Expense;
import ue.edu.co.splitbill.entity.ExpenseShare;
import ue.edu.co.splitbill.entity.Group;
import ue.edu.co.splitbill.entity.SyncStatus;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.manager.SplitBillDatabase;
import ue.edu.co.splitbill.network.ApiClient;
import ue.edu.co.splitbill.session.SessionManager;
import ue.edu.co.splitbill.session.TokenStore;
import ue.edu.co.splitbill.sync.SyncManager;
import ue.edu.co.splitbill.sync.SyncResult;

/**
 * Pruebas del SyncManager contra un servidor falso (MockWebServer) y una base de datos en memoria.
 *
 * El servidor falso responde lo que cada prueba le encola, en orden, y guarda las peticiones que
 * recibio: asi se verifica que la app suba las cosas en el orden correcto y que reaccione bien
 * cuando no hay red, cuando el servidor rechaza un cambio o cuando se cae.
 */
@RunWith(AndroidJUnit4.class)
public class SyncManagerTest {

    private static final String TOKEN = "token-de-prueba";

    private MockWebServer server;
    private SplitBillDatabase database;
    private SessionManager sessionManager;
    private SyncManager syncManager;

    private Group group;
    private User julian;
    private User diomar;
    private Expense almuerzo;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        this.server = new MockWebServer();
        this.server.start();
        this.database = Room.inMemoryDatabaseBuilder(context, SplitBillDatabase.class).build();

        this.sessionManager = new SessionManager(context, new MemoryTokenStore());
        this.sessionManager.clear();
        this.syncManager = new SyncManager(this.database,
                ApiClient.create(this.server.url("/").toString(), this.sessionManager),
                this.sessionManager, new AppExecutors());

        //estado tipico despues de iniciar sesion: grupo, integrantes y un gasto sin subir
        this.group = new Group("Paseo", "COP");
        this.database.groupDao().insert(this.group);
        this.julian = new User("Julian", "julian@test.com", null);
        this.julian.setSyncStatus(SyncStatus.SYNCED);
        this.diomar = new User("Diomar", null, null);
        this.database.userDao().insert(this.julian);
        this.database.userDao().insert(this.diomar);
        this.almuerzo = insertExpense("Almuerzo", 6_000_000L);

        this.sessionManager.startSession(TOKEN, this.julian.getId(), "Julian", "julian@test.com");
        this.sessionManager.setCurrentGroupId(this.group.getId());
    }

    @After
    public void tearDown() throws Exception {
        this.sessionManager.clear();
        this.database.close();
        this.server.shutdown();
    }

    @Test
    public void pushUploadsGroupThenMembersThenExpensesAndLeavesNothingPending() throws Exception {
        enqueue(201, groupJson());
        enqueue(201, userJson(this.diomar, true));
        enqueue(201, "{}");
        enqueue(200, "[" + userJson(this.julian, true) + "," + userJson(this.diomar, true) + "]");
        enqueue(200, "[" + expenseJson(this.almuerzo) + "]");

        SyncResult result = this.syncManager.syncNow();

        assertEquals(SyncResult.State.SYNCED, result.getState());
        assertEquals(0, result.getPendingChanges());
        assertRequest("POST", "/api/groups");
        assertRequest("POST", "/api/groups/" + this.group.getId() + "/members");
        RecordedRequest expense = assertRequest("POST", "/api/groups/" + this.group.getId() + "/expenses");
        String body = expense.getBody().readUtf8();
        assertTrue(body.contains(this.almuerzo.getId()));
        assertTrue(body.contains("\"amountCents\":3000000"));
        assertEquals("Bearer " + TOKEN, expense.getHeader("Authorization"));
        assertRequest("GET", "/api/groups/" + this.group.getId() + "/members?includeRemoved=true");
        assertRequest("GET", "/api/groups/" + this.group.getId() + "/expenses");
    }

    @Test
    public void withoutConnectionTheChangesStayPending() {
        //dos veces porque OkHttp reintenta una vez por su cuenta cuando se cae la conexion
        this.server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
        this.server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        SyncResult result = this.syncManager.syncNow();

        assertEquals(SyncResult.State.OFFLINE, result.getState());
        assertEquals(3, result.getPendingChanges());
    }

    @Test
    public void aServerFailureKeepsTheQueueForTheNextTry() {
        enqueue(500, "{\"status\":500,\"detail\":\"Ocurrió un error inesperado en el servidor\"}");

        SyncResult result = this.syncManager.syncNow();

        assertEquals(SyncResult.State.SERVER_ERROR, result.getState());
        assertEquals(3, result.getPendingChanges());
    }

    @Test
    public void aRejectedExpenseIsDiscardedAndTheUserIsTold() throws Exception {
        enqueue(201, groupJson());
        enqueue(201, userJson(this.diomar, true));
        enqueue(400, "{\"status\":400,\"detail\":\"Las partes suman 99 centavos pero el gasto es de 100 centavos\"}");
        enqueue(200, "[" + userJson(this.julian, true) + "," + userJson(this.diomar, true) + "]");
        enqueue(200, "[]");

        SyncResult result = this.syncManager.syncNow();

        assertEquals(SyncResult.State.SYNCED, result.getState());
        assertEquals(Arrays.asList("Las partes suman 99 centavos pero el gasto es de 100 centavos"),
                result.getRejectedMessages());
        //el servidor no lo tiene, asi que tampoco cuenta en el celular
        assertEquals(0, this.database.expenseDao().findActiveWithPayer(this.group.getId()).size());
        assertEquals(0, result.getPendingChanges());
    }

    @Test
    public void pullBringsNewExpensesAndRemovesTheOnesDeletedOnTheServer() throws Exception {
        //todo ya estaba sincronizado; en el servidor alguien borro el almuerzo y agrego un taxi
        this.database.groupDao().markSynced(this.group.getId());
        this.database.userDao().markSynced(this.diomar.getId(), this.diomar.getStatus());
        this.database.expenseDao().markSynced(this.almuerzo.getId(), this.almuerzo.getStatus());
        Expense taxi = new Expense(this.group.getId(), this.diomar.getId(), "Taxi", Money.ofCents(20_000L),
                SplitType.EQUAL);
        enqueue(200, "[" + userJson(this.julian, true) + "," + userJson(this.diomar, true) + "]");
        enqueue(200, "[" + expenseJson(taxi) + "]");

        SyncResult result = this.syncManager.syncNow();

        assertEquals(SyncResult.State.SYNCED, result.getState());
        assertEquals(1, this.database.expenseDao().findActiveWithPayer(this.group.getId()).size());
        assertEquals("Taxi", this.database.expenseDao().findById(taxi.getId()).getDescription());
        assertEquals(2, this.database.expenseShareDao().findByExpense(taxi.getId()).size());
        assertTrue(!this.database.expenseDao().findById(this.almuerzo.getId()).isActive());
    }

    @Test
    public void anEditedExpenseIsSentWithPut() throws Exception {
        //todo sincronizado; luego el usuario edita el almuerzo
        this.database.groupDao().markSynced(this.group.getId());
        this.database.userDao().markSynced(this.diomar.getId(), this.diomar.getStatus());
        this.almuerzo.setDescription("Almuerzo corregido");
        this.almuerzo.setSyncStatus(SyncStatus.PENDING_UPDATE);
        this.database.expenseDao().update(this.almuerzo);
        enqueue(200, expenseJson(this.almuerzo));
        enqueue(200, "[" + userJson(this.julian, true) + "," + userJson(this.diomar, true) + "]");
        enqueue(200, "[" + expenseJson(this.almuerzo) + "]");

        SyncResult result = this.syncManager.syncNow();

        assertEquals(SyncResult.State.SYNCED, result.getState());
        RecordedRequest put = assertRequest("PUT",
                "/api/groups/" + this.group.getId() + "/expenses/" + this.almuerzo.getId());
        assertTrue(put.getBody().readUtf8().contains("Almuerzo corregido"));
        assertEquals(0, result.getPendingChanges());
    }

    @Test
    public void editingAnExpenseDeletedByOthersIsDiscarded() throws Exception {
        this.database.groupDao().markSynced(this.group.getId());
        this.database.userDao().markSynced(this.diomar.getId(), this.diomar.getStatus());
        this.almuerzo.setSyncStatus(SyncStatus.PENDING_UPDATE);
        this.database.expenseDao().update(this.almuerzo);
        enqueue(404, "{\"status\":404,\"detail\":\"Gasto no encontrado\"}");
        enqueue(200, "[" + userJson(this.julian, true) + "," + userJson(this.diomar, true) + "]");
        enqueue(200, "[]");

        SyncResult result = this.syncManager.syncNow();

        assertEquals(Arrays.asList("Gasto no encontrado"), result.getRejectedMessages());
        assertTrue(!this.database.expenseDao().findById(this.almuerzo.getId()).isActive());
    }

    @Test
    public void anExpiredTokenStopsTheSyncAndClosesTheSession() {
        enqueue(401, "");

        SyncResult result = this.syncManager.syncNow();

        assertEquals(SyncResult.State.SESSION_EXPIRED, result.getState());
        assertNull(this.sessionManager.getToken());
        assertEquals(3, result.getPendingChanges());
    }

    // ------------------------------------------------------------------ utilidades

    private Expense insertExpense(String description, long cents) {
        Expense expense = new Expense(this.group.getId(), this.julian.getId(), description, Money.ofCents(cents),
                SplitType.EQUAL);
        this.database.expenseDao().insert(expense);
        this.database.expenseShareDao().insertAll(Arrays.asList(
                share(expense, this.julian.getId(), cents / 2),
                share(expense, this.diomar.getId(), cents - cents / 2)));
        return expense;
    }

    private static ExpenseShare share(Expense expense, String userId, long cents) {
        ExpenseShare share = new ExpenseShare();
        share.setExpenseId(expense.getId());
        share.setUserId(userId);
        share.setAmountCents(cents);
        return share;
    }

    private void enqueue(int code, String body) {
        this.server.enqueue(new MockResponse().setResponseCode(code)
                .setHeader("Content-Type", "application/json").setBody(body));
    }

    private RecordedRequest assertRequest(String method, String path) throws InterruptedException {
        RecordedRequest request = this.server.takeRequest();
        assertEquals(method, request.getMethod());
        assertEquals(path, request.getPath());
        return request;
    }

    private String groupJson() {
        return "{\"id\":\"" + this.group.getId() + "\",\"name\":\"Paseo\",\"currency\":\"COP\",\"ownerId\":\""
                + this.julian.getId() + "\"}";
    }

    private static String userJson(User user, boolean active) {
        return "{\"id\":\"" + user.getId() + "\",\"names\":\"" + user.getNames() + "\",\"registered\":false,"
                + "\"active\":" + active + "}";
    }

    private String expenseJson(Expense expense) {
        long half = expense.getAmountCents() / 2;
        return "{\"id\":\"" + expense.getId() + "\",\"groupId\":\"" + this.group.getId() + "\",\"payerId\":\""
                + expense.getPayerId() + "\",\"description\":\"" + expense.getDescription() + "\",\"amountCents\":"
                + expense.getAmountCents() + ",\"splitType\":\"EQUAL\",\"date\":\"2026-09-24T13:55:21.123456Z\","
                + "\"shares\":[{\"userId\":\"" + this.julian.getId() + "\",\"amountCents\":" + half + "},"
                + "{\"userId\":\"" + this.diomar.getId() + "\",\"amountCents\":"
                + (expense.getAmountCents() - half) + "}]}";
    }

    /** TokenStore de prueba: guarda el token en memoria en vez de cifrarlo con el Keystore. */
    private static class MemoryTokenStore implements TokenStore {

        private String token;

        @Override
        public void save(String token) {
            this.token = token;
        }

        @Override
        public String read() {
            return this.token;
        }

        @Override
        public void clear() {
            this.token = null;
        }
    }
}
