package ue.edu.co.splitbill.sync;

import android.util.Log;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Response;
import ue.edu.co.splitbill.di.AppExecutors;
import ue.edu.co.splitbill.entity.Expense;
import ue.edu.co.splitbill.entity.Group;
import ue.edu.co.splitbill.entity.GroupMember;
import ue.edu.co.splitbill.entity.QuickSplit;
import ue.edu.co.splitbill.entity.SyncStatus;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.manager.SplitBillDatabase;
import ue.edu.co.splitbill.network.ApiClient;
import ue.edu.co.splitbill.network.ApiException;
import ue.edu.co.splitbill.network.ApiMapper;
import ue.edu.co.splitbill.network.ApiService;
import ue.edu.co.splitbill.network.dto.ExpenseDto;
import ue.edu.co.splitbill.network.dto.GroupDto;
import ue.edu.co.splitbill.network.dto.QuickSplitDto;
import ue.edu.co.splitbill.network.dto.UserDto;
import ue.edu.co.splitbill.session.SessionManager;

/**
 * Mantiene la base de datos del celular y la del servidor iguales. Es el corazon del trabajo sin
 * conexion (offline-first):
 *
 * - Las pantallas SIEMPRE leen y escriben en Room, haya o no conexion. Cada cambio queda marcado con
 *   su sync_status (PENDING_CREATE, PENDING_DELETE): esa es la cola de cambios por enviar.
 * - Este SyncManager, en segundo plano, hace dos cosas en orden:
 *   1. Push: sube la cola de TODOS los grupos, respetando las dependencias (grupos, luego
 *      integrantes, luego gastos).
 *   2. Pull: trae la lista de grupos y lo que otros integrantes cambiaron en cada uno de ellos
 *      (primero el actual). Asi el inicio y la actividad muestran datos al dia de todos los grupos.
 *   Las cuentas rapidas guardadas no son de ningun grupo: se suben despues de los gastos y se traen
 *   al final, completas.
 *
 * Reglas:
 * - Nunca se pisa un cambio local pendiente con lo que llega del servidor.
 * - Si el servidor rechaza un cambio (4xx), no tiene sentido reintentarlo: se descarta, el pull
 *   restaura la version del servidor y se avisa al usuario.
 * - Si no hay red o el servidor falla (5xx), los cambios se quedan en la cola para el siguiente ciclo.
 * - Reintentar es seguro: el servidor reconoce el UUID de cada fila y no la duplica.
 */
public class SyncManager {

    private static final String TAG = "SyncManager";

    /**
     * Margen al pedir "lo que cambio desde": un gasto que se estaba guardando justo cuando se hizo la
     * consulta anterior puede tener una hora un poco anterior. Traerlo dos veces no hace dano.
     */
    private static final long PULL_SAFETY_MARGIN_MS = 60_000L;

    private final SplitBillDatabase database;
    private final ApiService api;
    private final SessionManager sessionManager;
    private final AppExecutors executors;

    /** Solo corre una sincronizacion a la vez; si piden otra mientras tanto, se repite al terminar. */
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean requestedAgain = new AtomicBoolean(false);
    private final List<SyncListener> listeners = new CopyOnWriteArrayList<>();

    /** Programa la sincronizacion en segundo plano (WorkManager). Null en las pruebas. */
    private Runnable backgroundScheduler;

    public SyncManager(SplitBillDatabase database, ApiService api, SessionManager sessionManager,
                       AppExecutors executors) {
        this.database = database;
        this.api = api;
        this.sessionManager = sessionManager;
        this.executors = executors;
    }

    public void addListener(SyncListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(SyncListener listener) {
        this.listeners.remove(listener);
    }

    public void setBackgroundScheduler(Runnable backgroundScheduler) {
        this.backgroundScheduler = backgroundScheduler;
    }

    /**
     * Lo llaman los repositorios despues de cada cambio local: se intenta subir ya mismo y, por si la
     * app se cierra antes o no hay red, queda programado para que Android lo haga cuando pueda.
     */
    public void notifyLocalChange() {
        requestSync();
        if (this.backgroundScheduler != null) {
            this.backgroundScheduler.run();
        }
    }

    /** Pide una sincronizacion en segundo plano. Se puede llamar las veces que sea, desde cualquier hilo. */
    public void requestSync() {
        if (!this.sessionManager.isLoggedIn()) {
            return;
        }
        this.requestedAgain.set(true);
        if (this.running.compareAndSet(false, true)) {
            this.executors.network().execute(this::runLoop);
        }
    }

    private void runLoop() {
        notifyStarted();
        SyncResult result;
        do {
            this.requestedAgain.set(false);
            result = syncNow();
        } while (this.requestedAgain.get() && result.isSynced());
        this.running.set(false);
        notifyFinished(result);
        //si alguien pidio sincronizar justo cuando se apagaba la bandera, no se pierde el pedido
        if (this.requestedAgain.get() && result.isSynced()) {
            requestSync();
        }
    }

    /**
     * Un ciclo completo de sincronizacion, en el hilo que lo llame. Es publico para las pruebas; la
     * app usa requestSync().
     */
    public synchronized SyncResult syncNow() {
        String groupId = this.sessionManager.getCurrentGroupId();
        if (this.sessionManager.getToken() == null || groupId == null) {
            return new SyncResult(SyncResult.State.SESSION_EXPIRED, countPending(), null);
        }
        List<String> rejected = new ArrayList<>();
        try {
            pushGroups(rejected);
            pushMembers(rejected);
            pushExpenses(rejected);
            pushQuickSplits(rejected);
            pullGroups();
            pullAllGroups(groupId);
            pullQuickSplits();
            return new SyncResult(SyncResult.State.SYNCED, countPending(), rejected);
        } catch (IOException e) {
            Log.e(TAG, "ERROR AL SINCRONIZAR: SIN CONEXION CON EL SERVIDOR", e);
            return new SyncResult(SyncResult.State.OFFLINE, countPending(), rejected);
        } catch (ApiException e) {
            Log.e(TAG, "ERROR AL SINCRONIZAR: EL SERVIDOR RESPONDIO " + e.getCode(), e);
            SyncResult.State state = e.isUnauthorized()
                    ? SyncResult.State.SESSION_EXPIRED : SyncResult.State.SERVER_ERROR;
            return new SyncResult(state, countPending(), rejected);
        }
    }

    public int countPending() {
        return this.database.groupDao().countPending()
                + this.database.groupMemberDao().countPending()
                + this.database.expenseDao().countPending()
                + this.database.quickSplitDao().countPending();
    }

    // ------------------------------------------------------------------ push

    private void pushGroups(List<String> rejected) throws IOException {
        for (Group group : this.database.groupDao().findPending()) {
            try {
                GroupDto saved = group.getSyncStatus() == SyncStatus.PENDING_UPDATE
                        ? ApiClient.execute(this.api.updateGroup(group.getId(), ApiMapper.toDto(group)))
                        : ApiClient.execute(this.api.createGroup(ApiMapper.toDto(group)));
                //el servidor devuelve el dueno; se guarda la version del servidor, ya sincronizada
                this.database.groupDao().upsert(ApiMapper.toEntity(saved));
            } catch (ApiException e) {
                discardIfRejected(e, rejected);
                this.database.groupDao().markSynced(group.getId());
            }
        }
    }

    /** Cada pertenencia pendiente se sube a su propio grupo, no necesariamente al actual. */
    private void pushMembers(List<String> rejected) throws IOException {
        for (GroupMember member : this.database.groupMemberDao().findPending()) {
            User user = this.database.userDao().findById(member.getUserId());
            try {
                if (member.getSyncStatus() == SyncStatus.PENDING_CREATE && user != null) {
                    ApiClient.execute(this.api.addMember(member.getGroupId(), ApiMapper.toMemberRequest(user)));
                }
                if (!member.isActive()) {
                    deleteIgnoringNotFound(this.api.removeMember(member.getGroupId(), member.getUserId()));
                }
            } catch (ApiException e) {
                discardIfRejected(e, rejected);
            }
            this.database.groupMemberDao().markSynced(member.getGroupId(), member.getUserId(), member.getStatus());
        }
    }

    private void pushExpenses(List<String> rejected) throws IOException {
        for (Expense expense : this.database.expenseDao().findPending()) {
            try {
                if (expense.getSyncStatus() == SyncStatus.PENDING_CREATE) {
                    ExpenseDto dto = ApiMapper.toDto(expense,
                            this.database.expenseShareDao().findByExpense(expense.getId()));
                    ApiClient.execute(this.api.createExpense(expense.getGroupId(), dto));
                } else if (expense.getSyncStatus() == SyncStatus.PENDING_UPDATE && expense.isActive()) {
                    //si otro integrante ya lo borro, el servidor responde 404: se descarta y el pull lo quita
                    ExpenseDto dto = ApiMapper.toDto(expense,
                            this.database.expenseShareDao().findByExpense(expense.getId()));
                    ApiClient.execute(this.api.updateExpense(expense.getGroupId(), expense.getId(), dto));
                }
                if (!expense.isActive()) {
                    deleteIgnoringNotFound(this.api.deleteExpense(expense.getGroupId(), expense.getId()));
                }
            } catch (ApiException e) {
                discardIfRejected(e, rejected);
            }
            this.database.expenseDao().markSynced(expense.getId(), expense.getStatus());
        }
    }

    /** Las cuentas rapidas no se editan: se crean o se borran. */
    private void pushQuickSplits(List<String> rejected) throws IOException {
        for (QuickSplit quickSplit : this.database.quickSplitDao().findPending()) {
            try {
                if (quickSplit.getSyncStatus() == SyncStatus.PENDING_CREATE) {
                    QuickSplitDto dto = ApiMapper.toDto(quickSplit,
                            this.database.quickSplitDao().findShares(quickSplit.getId()));
                    ApiClient.execute(this.api.createQuickSplit(dto));
                }
                if (!quickSplit.isActive()) {
                    deleteIgnoringNotFound(this.api.deleteQuickSplit(quickSplit.getId()));
                }
            } catch (ApiException e) {
                discardIfRejected(e, rejected);
            }
            this.database.quickSplitDao().markSynced(quickSplit.getId(), quickSplit.getStatus());
        }
    }

    /** Borrar algo que el servidor ya no tiene cuenta como exito: el resultado es el mismo. */
    private void deleteIgnoringNotFound(retrofit2.Call<Void> call) throws IOException {
        try {
            ApiClient.execute(call);
        } catch (ApiException e) {
            if (!e.isNotFound()) {
                throw e;
            }
        }
    }

    /**
     * Un rechazo definitivo (4xx) se anota para el usuario y el cambio se descarta. Cualquier otro
     * error (401, 5xx) corta la sincronizacion y el cambio sigue en la cola.
     */
    private void discardIfRejected(ApiException e, List<String> rejected) {
        if (!e.isPermanent()) {
            throw e;
        }
        Log.e(TAG, "ERROR AL SUBIR UN CAMBIO: EL SERVIDOR LO RECHAZO", e);
        rejected.add(e.getMessage());
    }

    // ------------------------------------------------------------------ pull

    /**
     * Trae los grupos de la persona: los nuevos (por ejemplo uno al que la agregaron desde otro
     * celular) aparecen, y los que el servidor ya no devuelve dejan de mostrarse. Un grupo con
     * cambios locales sin subir no se toca.
     */
    private void pullGroups() throws IOException {
        final List<GroupDto> groups = ApiClient.execute(this.api.getGroups());
        this.database.runInTransaction(new Runnable() {
            @Override
            public void run() {
                Set<String> serverIds = new HashSet<>();
                for (GroupDto dto : groups) {
                    serverIds.add(dto.getId());
                    Group local = database.groupDao().findById(dto.getId());
                    if (local == null || local.getSyncStatus() == SyncStatus.SYNCED) {
                        database.groupDao().upsert(ApiMapper.toEntity(dto));
                    }
                }
                for (String groupId : database.groupDao().findSyncedActiveIds()) {
                    if (!serverIds.contains(groupId)) {
                        database.groupDao().markRemovedByServer(groupId);
                    }
                }
            }
        });
    }

    /**
     * Trae los cambios de todos los grupos que el servidor conoce, empezando por el actual: si la red
     * se cae a mitad de camino, al menos la pantalla abierta ya quedo al dia. Cada grupo lleva su
     * propia hora de ultima sincronizacion, asi que despues de la primera vez solo viaja lo que cambio.
     * Un grupo recien creado que aun no se sube no se pide (el servidor todavia no lo tiene).
     */
    private void pullAllGroups(String currentGroupId) throws IOException {
        List<String> groupIds = this.database.groupDao().findSyncedActiveIds();
        if (groupIds.remove(currentGroupId)) {
            pull(currentGroupId);
        }
        for (String groupId : groupIds) {
            pull(groupId);
        }
    }

    /**
     * Trae los cambios del servidor en un grupo. Los integrantes llegan siempre completos (son pocos). Los gastos
     * llegan completos la primera vez; despues, solo los que cambiaron desde la ultima sincronizacion,
     * incluidos los borrados.
     */
    private void pull(final String groupId) throws IOException {
        final List<UserDto> members = ApiClient.execute(this.api.getMembers(groupId, true));
        long lastPull = this.sessionManager.getLastPull(groupId);
        final boolean incremental = lastPull > 0;
        String since = incremental ? Instant.ofEpochMilli(lastPull).toString() : null;
        Response<List<ExpenseDto>> response = ApiClient.executeForResponse(this.api.getExpenses(groupId, since));
        final List<ExpenseDto> expenses = response.body() == null ? new ArrayList<ExpenseDto>() : response.body();

        //se aplica todo en una transaccion: la pantalla nunca ve la mitad de un pull
        this.database.runInTransaction(new Runnable() {
            @Override
            public void run() {
                for (UserDto dto : members) {
                    //los datos de la persona los manda el servidor; su pertenencia solo si no hay cambio local
                    database.userDao().upsert(ApiMapper.toEntity(dto));
                    GroupMember local = database.groupMemberDao().findById(groupId, dto.getId());
                    if (local == null || local.getSyncStatus() == SyncStatus.SYNCED) {
                        database.groupMemberDao().upsert(ApiMapper.toMember(groupId, dto));
                    }
                }

                Set<String> serverIds = new HashSet<>();
                for (ExpenseDto dto : expenses) {
                    serverIds.add(dto.getId());
                    Expense local = database.expenseDao().findById(dto.getId());
                    if (local != null && local.getSyncStatus() != SyncStatus.SYNCED) {
                        //hay un cambio local sin subir: no se pisa
                        continue;
                    }
                    if (dto.isActive()) {
                        database.expenseDao().upsert(ApiMapper.toEntity(dto));
                        database.expenseShareDao().deleteByExpense(dto.getId());
                        database.expenseShareDao().insertAll(ApiMapper.toShares(dto));
                    } else if (local != null) {
                        //otro integrante lo borro
                        database.expenseDao().markDeletedByServer(dto.getId());
                    }
                }

                //en la lista completa los borrados no vienen: se deducen de los que faltan
                if (!incremental) {
                    for (String expenseId : database.expenseDao().findSyncedActiveIds(groupId)) {
                        if (!serverIds.contains(expenseId)) {
                            database.expenseDao().markDeletedByServer(expenseId);
                        }
                    }
                }
            }
        });

        //la proxima vez se pide solo lo que cambie desde ahora, con la hora del servidor (no la del celular)
        Date serverDate = response.headers().getDate("Date");
        if (serverDate != null) {
            this.sessionManager.setLastPull(groupId, serverDate.getTime() - PULL_SAFETY_MARGIN_MS);
        }
    }

    /**
     * Trae las cuentas rapidas guardadas (por ejemplo, las que se guardaron en otro celular). Llega la
     * lista completa: las que faltan se borraron en otro lado. Una con cambios sin subir no se toca.
     */
    private void pullQuickSplits() throws IOException {
        final List<QuickSplitDto> quickSplits = ApiClient.execute(this.api.getQuickSplits());
        this.database.runInTransaction(new Runnable() {
            @Override
            public void run() {
                Set<String> serverIds = new HashSet<>();
                for (QuickSplitDto dto : quickSplits) {
                    serverIds.add(dto.getId());
                    QuickSplit local = database.quickSplitDao().findById(dto.getId());
                    if (local != null && local.getSyncStatus() != SyncStatus.SYNCED) {
                        continue;
                    }
                    database.quickSplitDao().upsert(ApiMapper.toEntity(dto));
                    database.quickSplitDao().deleteShares(dto.getId());
                    database.quickSplitDao().insertShares(ApiMapper.toQuickSplitShares(dto));
                }
                for (String quickSplitId : database.quickSplitDao().findSyncedActiveIds()) {
                    if (!serverIds.contains(quickSplitId)) {
                        database.quickSplitDao().markDeletedByServer(quickSplitId);
                    }
                }
            }
        });
    }

    // ------------------------------------------------------------------ avisos a la pantalla

    private void notifyStarted() {
        this.executors.mainThread().execute(new Runnable() {
            @Override
            public void run() {
                for (SyncListener listener : listeners) {
                    listener.onSyncStarted();
                }
            }
        });
    }

    private void notifyFinished(final SyncResult result) {
        this.executors.mainThread().execute(new Runnable() {
            @Override
            public void run() {
                for (SyncListener listener : listeners) {
                    listener.onSyncFinished(result);
                }
            }
        });
    }
}
