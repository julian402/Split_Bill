package ue.edu.co.splitbill.model;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import ue.edu.co.splitbill.di.AppExecutors;
import ue.edu.co.splitbill.entity.Group;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.manager.DatabaseContract;
import ue.edu.co.splitbill.manager.SplitBillDatabase;
import ue.edu.co.splitbill.network.ApiClient;
import ue.edu.co.splitbill.network.ApiMapper;
import ue.edu.co.splitbill.network.ApiService;
import ue.edu.co.splitbill.network.dto.GroupDto;
import ue.edu.co.splitbill.network.dto.LoginRequest;
import ue.edu.co.splitbill.network.dto.RegisterRequest;
import ue.edu.co.splitbill.network.dto.TokenResponse;
import ue.edu.co.splitbill.session.SessionManager;
import ue.edu.co.splitbill.sync.SyncManager;

/**
 * Inicio de sesion, registro y cierre de sesion.
 *
 * Iniciar sesion no es solo guardar el token: hay que decidir en que grupo va a trabajar la persona
 * y dejar lista la base de datos local para sincronizar (ver prepareGroup).
 */
public class SessionRepository extends BaseRepository {

    private static final String TAG = "SessionRepository";
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final ApiService api;
    private final SessionManager sessionManager;
    private final SyncManager syncManager;

    public SessionRepository(SplitBillDatabase database, AppExecutors executors, ApiService api,
                             SessionManager sessionManager, SyncManager syncManager) {
        super(database, executors);
        this.api = api;
        this.sessionManager = sessionManager;
        this.syncManager = syncManager;
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    public boolean isLoggedIn() {
        return this.sessionManager.isLoggedIn();
    }

    /** @param callback recibe el nombre de la persona para saludarla */
    public void login(final String email, final String password, DataCallback<String> callback) {
        runNetwork(new Callable<String>() {
            @Override
            public String call() throws IOException {
                validateEmail(email);
                if (password == null || password.isEmpty()) {
                    throw new IllegalArgumentException("Escribe tu contrasena");
                }
                TokenResponse response = ApiClient.execute(api.login(new LoginRequest(email.trim(), password)));
                return startSession(response);
            }
        }, callback);
    }

    public void register(final String names, final String email, final String phone, final String password,
                         DataCallback<String> callback) {
        runNetwork(new Callable<String>() {
            @Override
            public String call() throws IOException {
                if (names == null || names.trim().length() < 2) {
                    throw new IllegalArgumentException("Escribe tu nombre");
                }
                validateEmail(email);
                if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
                    throw new IllegalArgumentException("La contrasena debe tener al menos 8 caracteres");
                }
                String cleanPhone = phone == null || phone.trim().isEmpty() ? null : phone.trim();
                TokenResponse response = ApiClient.execute(api.register(
                        new RegisterRequest(names.trim(), email.trim(), cleanPhone, password)));
                return startSession(response);
            }
        }, callback);
    }

    /**
     * Cierra la sesion y borra los datos del celular: otra persona que use este telefono no debe
     * ver los gastos de la anterior. Lo que ya estaba sincronizado sigue a salvo en el servidor.
     */
    public void logout(DataCallback<Boolean> callback) {
        runAsync(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                database.clearAllTables();
                sessionManager.clear();
                return true;
            }
        }, callback);
    }

    /** Cambios que se perderian si se cierra la sesion ahora. */
    public void countPendingChanges(DataCallback<Integer> callback) {
        runAsync(new Callable<Integer>() {
            @Override
            public Integer call() {
                return syncManager.countPending();
            }
        }, callback);
    }

    private String startSession(TokenResponse response) throws IOException {
        String userId = response.getUser().getId();

        //si antes habia entrado otra persona en este celular, sus datos no se mezclan con los nuevos
        String previousUserId = this.sessionManager.getUserId();
        if (previousUserId != null && !previousUserId.equals(userId)) {
            this.database.clearAllTables();
            this.sessionManager.clear();
        }

        this.sessionManager.startSession(response.getToken(), userId, response.getUser().getNames(),
                response.getUser().getEmail());

        //quien inicia sesion es integrante de su grupo y puede ser el pagador de un gasto
        this.database.userDao().upsert(ApiMapper.toEntity(response.getUser()));

        this.sessionManager.setCurrentGroupId(prepareGroup(userId));
        this.syncManager.requestSync();
        return response.getUser().getNames();
    }

    /**
     * Decide en que grupo trabaja la persona:
     *
     * 1. Si ya tenia un grupo en este celular (volvio a entrar despues de que vencio el token), sigue en el.
     * 2. Si el celular trae datos de la entrega 1 en el grupo sembrado, ese grupo se vuelve suyo. Como
     *    todas las instalaciones sembraron el grupo con el mismo id, primero recibe un UUID nuevo; luego
     *    el SyncManager lo sube con sus integrantes y gastos.
     * 3. Si no, se usa su grupo mas reciente del servidor.
     * 4. Si no tiene ninguno, se crea "Mi grupo", que se subira en la siguiente sincronizacion.
     */
    private String prepareGroup(final String userId) throws IOException {
        String currentGroupId = this.sessionManager.getCurrentGroupId();
        if (currentGroupId != null && this.database.groupDao().findById(currentGroupId) != null) {
            return currentGroupId;
        }

        final String seededId = DatabaseContract.DEFAULT_GROUP_ID;
        boolean hasSeededGroup = this.database.groupDao().findById(seededId) != null;
        //la persona que acaba de iniciar sesion ya esta en users, por eso se compara con 1 y no con 0
        boolean hasLocalData = this.database.expenseDao().countAll() > 0 || this.database.userDao().countAll() > 1;

        if (hasSeededGroup && hasLocalData) {
            final String newId = UUID.randomUUID().toString();
            this.database.runInTransaction(new Runnable() {
                @Override
                public void run() {
                    database.groupDao().copyWithNewId(seededId, newId, userId);
                    database.groupDao().moveExpenses(seededId, newId);
                    database.groupDao().deleteById(seededId);
                }
            });
            return newId;
        }
        if (hasSeededGroup) {
            this.database.groupDao().deleteById(seededId);
        }

        List<GroupDto> serverGroups = ApiClient.execute(this.api.getGroups());
        if (!serverGroups.isEmpty()) {
            //el servidor los devuelve del mas reciente al mas antiguo
            Group group = ApiMapper.toEntity(serverGroups.get(0));
            this.database.groupDao().upsert(group);
            return group.getId();
        }

        Group group = new Group(DatabaseContract.DEFAULT_GROUP_NAME, DatabaseContract.DEFAULT_GROUP_CURRENCY);
        group.setOwnerId(userId);
        this.database.groupDao().insert(group);
        return group.getId();
    }

    private static void validateEmail(String email) {
        if (email == null || !email.trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new IllegalArgumentException("Escribe un email valido");
        }
    }
}
