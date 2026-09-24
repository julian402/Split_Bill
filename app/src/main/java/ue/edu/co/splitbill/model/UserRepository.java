package ue.edu.co.splitbill.model;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import ue.edu.co.splitbill.di.AppExecutors;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.manager.SplitBillDatabase;
import ue.edu.co.splitbill.network.ApiClient;
import ue.edu.co.splitbill.network.ApiService;
import ue.edu.co.splitbill.session.SessionManager;
import ue.edu.co.splitbill.sync.SyncResult;
import ue.edu.co.splitbill.sync.SyncManager;

/**
 * Repositorio de integrantes del grupo.
 *
 * Es la unica puerta de entrada a la tabla de usuarios: las pantallas no conocen el DAO ni la base
 * de datos, solo piden datos y reciben la respuesta por el callback.
 */
public class UserRepository extends BaseRepository {

    private static final String TAG = "UserRepository";

    private final SyncManager syncManager;
    private final ApiService api;
    private final SessionManager sessionManager;

    /** @param syncManager se le avisa despues de cada cambio para que lo suba al servidor cuando pueda */
    public UserRepository(SplitBillDatabase database, AppExecutors executors, SyncManager syncManager,
                          ApiService api, SessionManager sessionManager) {
        super(database, executors);
        this.syncManager = syncManager;
        this.api = api;
        this.sessionManager = sessionManager;
    }

    /** Id de quien inicio sesion, para no ofrecerle "Soy yo" sobre si mismo. */
    public String getCurrentUserId() {
        return this.sessionManager.getUserId();
    }

    /**
     * "Soy yo": el integrante sin cuenta es la persona que inicio sesion. El servidor pasa sus gastos y
     * partes a la cuenta; luego se sincroniza para traer el resultado.
     *
     * Es de las pocas operaciones que necesitan conexion, porque cambia gastos de todo el grupo. Antes
     * se sube lo pendiente, para que el servidor junte todo, incluido lo registrado sin conexion.
     */
    public void claimMember(final String memberId, DataCallback<Boolean> callback) {
        runNetwork(new Callable<Boolean>() {
            @Override
            public Boolean call() throws IOException {
                SyncResult before = syncManager.syncNow();
                if (!before.isSynced()) {
                    throw new IOException("No se pudo sincronizar antes de juntar los integrantes");
                }
                ApiClient.execute(api.claimMember(sessionManager.getCurrentGroupId(), memberId));
                syncManager.syncNow();
                return true;
            }
        }, callback);
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    /** Registra un integrante nuevo. Valida antes de tocar la base de datos. */
    public void insertUser(final User user, DataCallback<User> callback) {
        runAsync(new Callable<User>() {
            @Override
            public User call() {
                user.validar();
                database.userDao().insert(user);
                syncManager.requestSync();
                return user;
            }
        }, callback);
    }

    public void getActiveUsers(DataCallback<List<User>> callback) {
        runAsync(new Callable<List<User>>() {
            @Override
            public List<User> call() {
                return database.userDao().findActive();
            }
        }, callback);
    }

    public void searchUserById(final String userId, DataCallback<User> callback) {
        runAsync(new Callable<User>() {
            @Override
            public User call() {
                User user = database.userDao().findById(userId);
                if (user == null) {
                    throw new IllegalArgumentException("No se encontró el integrante");
                }
                return user;
            }
        }, callback);
    }

    public void updateUser(final User user, DataCallback<User> callback) {
        runAsync(new Callable<User>() {
            @Override
            public User call() {
                user.validar();
                database.userDao().update(user);
                return user;
            }
        }, callback);
    }

    /** Borrado logico: el integrante deja de aparecer pero sus gastos siguen cuadrando. */
    public void deleteUser(final String userId, DataCallback<Integer> callback) {
        runAsync(new Callable<Integer>() {
            @Override
            public Integer call() {
                int rowsAffected = database.userDao().softDelete(userId);
                if (rowsAffected == 0) {
                    throw new IllegalArgumentException("No se encontró el integrante");
                }
                syncManager.requestSync();
                return rowsAffected;
            }
        }, callback);
    }

    public void countActiveUsers(DataCallback<Integer> callback) {
        runAsync(new Callable<Integer>() {
            @Override
            public Integer call() {
                return database.userDao().countActive();
            }
        }, callback);
    }
}
