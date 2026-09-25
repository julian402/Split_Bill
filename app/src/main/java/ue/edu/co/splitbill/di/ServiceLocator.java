package ue.edu.co.splitbill.di;

import android.content.Context;

import ue.edu.co.splitbill.BuildConfig;
import ue.edu.co.splitbill.domain.BalanceCalculator;
import ue.edu.co.splitbill.domain.DebtSimplifier;
import ue.edu.co.splitbill.manager.SplitBillDatabase;
import ue.edu.co.splitbill.model.ContactRepository;
import ue.edu.co.splitbill.model.DashboardRepository;
import ue.edu.co.splitbill.model.ExpenseRepository;
import ue.edu.co.splitbill.model.GroupRepository;
import ue.edu.co.splitbill.model.QuickSplitRepository;
import ue.edu.co.splitbill.model.SessionRepository;
import ue.edu.co.splitbill.model.SettlementRepository;
import ue.edu.co.splitbill.model.UserRepository;
import ue.edu.co.splitbill.network.ApiClient;
import ue.edu.co.splitbill.network.ApiService;
import ue.edu.co.splitbill.session.KeystoreTokenStore;
import ue.edu.co.splitbill.session.SessionManager;
import ue.edu.co.splitbill.session.TokenStore;
import ue.edu.co.splitbill.sync.NetworkMonitor;
import ue.edu.co.splitbill.sync.SyncManager;
import ue.edu.co.splitbill.sync.SyncScheduler;

/**
 * Arma los objetos de la aplicacion y decide cuales se comparten.
 *
 * Sin esta clase cada Activity haria "new UserRepository(context)" en cada metodo, lo que significa
 * abrir varias conexiones a la misma base de datos y crear varios pools de hilos. Aqui cada pieza se
 * construye una sola vez, la primera vez que alguien la pide, y se reutiliza.
 *
 * Es inyeccion de dependencias hecha a mano: las clases no salen a buscar lo que necesitan, lo
 * reciben por constructor. Eso permite, por ejemplo, pasarle a toda la app una base de datos en
 * memoria durante las pruebas de interfaz (ver el segundo constructor).
 */
public class ServiceLocator {

    private final Context context;

    /** Lo que las pruebas de interfaz pueden cambiar. Null = lo normal de la app. */
    private final String apiBaseUrl;
    private final TokenStore tokenStore;

    private SplitBillDatabase database;
    private AppExecutors executors;
    private SessionManager sessionManager;
    private ApiService apiService;
    private SyncManager syncManager;
    private NetworkMonitor networkMonitor;
    private UserRepository userRepository;
    private ExpenseRepository expenseRepository;
    private SettlementRepository settlementRepository;
    private SessionRepository sessionRepository;
    private ContactRepository contactRepository;
    private GroupRepository groupRepository;
    private DashboardRepository dashboardRepository;
    private QuickSplitRepository quickSplitRepository;

    public ServiceLocator(Context context) {
        this(context, null, null, null, null);
    }

    /**
     * Para las pruebas de interfaz (Espresso): una base de datos en memoria, el token en memoria, un
     * servidor falso y hilos que Espresso sabe esperar. Asi las pruebas no tocan los datos reales del
     * celular ni necesitan el backend.
     */
    public ServiceLocator(Context context, SplitBillDatabase database, TokenStore tokenStore,
                          AppExecutors executors, String apiBaseUrl) {
        this.context = context.getApplicationContext();
        this.database = database;
        this.tokenStore = tokenStore;
        this.executors = executors;
        this.apiBaseUrl = apiBaseUrl;
    }

    public synchronized SplitBillDatabase getDatabase() {
        if (this.database == null) {
            this.database = SplitBillDatabase.getInstance(this.context);
        }
        return this.database;
    }

    public synchronized AppExecutors getExecutors() {
        if (this.executors == null) {
            this.executors = new AppExecutors();
        }
        return this.executors;
    }

    public synchronized SessionManager getSessionManager() {
        if (this.sessionManager == null) {
            TokenStore store = this.tokenStore != null ? this.tokenStore : new KeystoreTokenStore(this.context);
            this.sessionManager = new SessionManager(this.context, store);
        }
        return this.sessionManager;
    }

    /** La direccion del backend sale de BuildConfig: distinta para desarrollo y para produccion. */
    public synchronized ApiService getApiService() {
        if (this.apiService == null) {
            String baseUrl = this.apiBaseUrl != null ? this.apiBaseUrl : BuildConfig.API_BASE_URL;
            this.apiService = ApiClient.create(baseUrl, getSessionManager());
        }
        return this.apiService;
    }

    public synchronized SyncManager getSyncManager() {
        if (this.syncManager == null) {
            this.syncManager = new SyncManager(getDatabase(), getApiService(), getSessionManager(), getExecutors());
            //ademas de sincronizar con la app abierta, deja programado el trabajo en segundo plano
            this.syncManager.setBackgroundScheduler(new SyncScheduler(this.context)::schedule);
        }
        return this.syncManager;
    }

    public synchronized NetworkMonitor getNetworkMonitor() {
        if (this.networkMonitor == null) {
            this.networkMonitor = new NetworkMonitor(this.context);
        }
        return this.networkMonitor;
    }

    public synchronized UserRepository getUserRepository() {
        if (this.userRepository == null) {
            this.userRepository = new UserRepository(getDatabase(), getExecutors(), getSyncManager(),
                    getApiService(), getSessionManager());
        }
        return this.userRepository;
    }

    public synchronized ExpenseRepository getExpenseRepository() {
        if (this.expenseRepository == null) {
            this.expenseRepository = new ExpenseRepository(getDatabase(), getExecutors(), getSyncManager());
        }
        return this.expenseRepository;
    }

    public synchronized SettlementRepository getSettlementRepository() {
        if (this.settlementRepository == null) {
            this.settlementRepository = new SettlementRepository(
                    getDatabase(),
                    getExecutors(),
                    new BalanceCalculator(),
                    new DebtSimplifier(),
                    getSyncManager());
        }
        return this.settlementRepository;
    }

    public synchronized SessionRepository getSessionRepository() {
        if (this.sessionRepository == null) {
            this.sessionRepository = new SessionRepository(getDatabase(), getExecutors(), getApiService(),
                    getSessionManager(), getSyncManager());
        }
        return this.sessionRepository;
    }

    public synchronized ContactRepository getContactRepository() {
        if (this.contactRepository == null) {
            this.contactRepository = new ContactRepository(getDatabase(), getExecutors(),
                    this.context.getContentResolver());
        }
        return this.contactRepository;
    }

    public synchronized GroupRepository getGroupRepository() {
        if (this.groupRepository == null) {
            this.groupRepository = new GroupRepository(getDatabase(), getExecutors(), getSessionManager(),
                    getSyncManager());
        }
        return this.groupRepository;
    }

    public synchronized DashboardRepository getDashboardRepository() {
        if (this.dashboardRepository == null) {
            this.dashboardRepository = new DashboardRepository(getDatabase(), getExecutors(), getSessionManager());
        }
        return this.dashboardRepository;
    }

    public synchronized QuickSplitRepository getQuickSplitRepository() {
        if (this.quickSplitRepository == null) {
            this.quickSplitRepository = new QuickSplitRepository(getDatabase(), getExecutors(), getSyncManager());
        }
        return this.quickSplitRepository;
    }
}
