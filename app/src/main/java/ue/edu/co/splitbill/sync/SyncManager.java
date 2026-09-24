package ue.edu.co.splitbill.sync;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import ue.edu.co.splitbill.di.AppExecutors;
import ue.edu.co.splitbill.entity.Expense;
import ue.edu.co.splitbill.entity.Group;
import ue.edu.co.splitbill.entity.SyncStatus;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.manager.SplitBillDatabase;
import ue.edu.co.splitbill.network.ApiClient;
import ue.edu.co.splitbill.network.ApiException;
import ue.edu.co.splitbill.network.ApiMapper;
import ue.edu.co.splitbill.network.ApiService;
import ue.edu.co.splitbill.network.dto.ExpenseDto;
import ue.edu.co.splitbill.network.dto.GroupDto;
import ue.edu.co.splitbill.network.dto.UserDto;
import ue.edu.co.splitbill.session.SessionManager;

/**
 * Mantiene la base de datos del celular y la del servidor iguales. Es el corazon del trabajo sin
 * conexion (offline-first):
 *
 * - Las pantallas SIEMPRE leen y escriben en Room, haya o no conexion. Cada cambio queda marcado con
 *   su sync_status (PENDING_CREATE, PENDING_DELETE): esa es la cola de cambios por enviar.
 * - Este SyncManager, en segundo plano, hace dos cosas en orden:
 *   1. Push: sube la cola, respetando las dependencias (grupo, luego integrantes, luego gastos).
 *   2. Pull: trae lo que otros integrantes cambiaron en el servidor.
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

    private final SplitBillDatabase database;
    private final ApiService api;
    private final SessionManager sessionManager;
    private final AppExecutors executors;

    /** Solo corre una sincronizacion a la vez; si piden otra mientras tanto, se repite al terminar. */
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean requestedAgain = new AtomicBoolean(false);
    private final List<SyncListener> listeners = new CopyOnWriteArrayList<>();

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
    public SyncResult syncNow() {
        String groupId = this.sessionManager.getCurrentGroupId();
        if (this.sessionManager.getToken() == null || groupId == null) {
            return new SyncResult(SyncResult.State.SESSION_EXPIRED, countPending(), null);
        }
        List<String> rejected = new ArrayList<>();
        try {
            pushGroups(rejected);
            pushMembers(groupId, rejected);
            pushExpenses(rejected);
            pull(groupId);
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
                + this.database.userDao().countPending()
                + this.database.expenseDao().countPending();
    }

    // ------------------------------------------------------------------ push

    private void pushGroups(List<String> rejected) throws IOException {
        for (Group group : this.database.groupDao().findPending()) {
            try {
                GroupDto saved = ApiClient.execute(this.api.createGroup(ApiMapper.toDto(group)));
                //el servidor devuelve el dueno; se guarda la version del servidor, ya sincronizada
                this.database.groupDao().upsert(ApiMapper.toEntity(saved));
            } catch (ApiException e) {
                discardIfRejected(e, rejected);
                this.database.groupDao().markSynced(group.getId());
            }
        }
    }

    private void pushMembers(String groupId, List<String> rejected) throws IOException {
        for (User user : this.database.userDao().findPending()) {
            try {
                if (user.getSyncStatus() == SyncStatus.PENDING_CREATE) {
                    ApiClient.execute(this.api.addMember(groupId, ApiMapper.toMemberRequest(user)));
                }
                if (!user.isActive()) {
                    deleteIgnoringNotFound(this.api.removeMember(groupId, user.getId()));
                }
            } catch (ApiException e) {
                discardIfRejected(e, rejected);
            }
            this.database.userDao().markSynced(user.getId(), user.getStatus());
        }
    }

    private void pushExpenses(List<String> rejected) throws IOException {
        for (Expense expense : this.database.expenseDao().findPending()) {
            try {
                if (expense.getSyncStatus() == SyncStatus.PENDING_CREATE) {
                    ExpenseDto dto = ApiMapper.toDto(expense,
                            this.database.expenseShareDao().findByExpense(expense.getId()));
                    ApiClient.execute(this.api.createExpense(expense.getGroupId(), dto));
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

    private void pull(final String groupId) throws IOException {
        final List<UserDto> members = ApiClient.execute(this.api.getMembers(groupId, true));
        final List<ExpenseDto> expenses = ApiClient.execute(this.api.getExpenses(groupId));

        //se aplica todo en una transaccion: la pantalla nunca ve la mitad de un pull
        this.database.runInTransaction(new Runnable() {
            @Override
            public void run() {
                for (UserDto dto : members) {
                    User local = database.userDao().findById(dto.getId());
                    if (local == null || local.getSyncStatus() == SyncStatus.SYNCED) {
                        database.userDao().upsert(ApiMapper.toEntity(dto));
                    }
                }

                Set<String> serverIds = new HashSet<>();
                for (ExpenseDto dto : expenses) {
                    serverIds.add(dto.getId());
                    Expense local = database.expenseDao().findById(dto.getId());
                    if (local == null || local.getSyncStatus() == SyncStatus.SYNCED) {
                        database.expenseDao().upsert(ApiMapper.toEntity(dto));
                        database.expenseShareDao().deleteByExpense(dto.getId());
                        database.expenseShareDao().insertAll(ApiMapper.toShares(dto));
                    }
                }

                //los gastos que estaban sincronizados y ya no vienen del servidor, otro los borro
                for (String expenseId : database.expenseDao().findSyncedActiveIds(groupId)) {
                    if (!serverIds.contains(expenseId)) {
                        database.expenseDao().markDeletedByServer(expenseId);
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
