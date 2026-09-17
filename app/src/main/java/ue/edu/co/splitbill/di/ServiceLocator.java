package ue.edu.co.splitbill.di;

import android.content.Context;

import ue.edu.co.splitbill.domain.BalanceCalculator;
import ue.edu.co.splitbill.domain.DebtSimplifier;
import ue.edu.co.splitbill.manager.SplitBillDatabase;
import ue.edu.co.splitbill.model.ExpenseRepository;
import ue.edu.co.splitbill.model.SettlementRepository;
import ue.edu.co.splitbill.model.UserRepository;

/**
 * Arma los objetos de la aplicacion y decide cuales se comparten.
 *
 * Sin esta clase cada Activity haria "new UserRepository(context)" en cada metodo, lo que significa
 * abrir varias conexiones a la misma base de datos y crear varios pools de hilos. Aqui cada pieza se
 * construye una sola vez, la primera vez que alguien la pide, y se reutiliza.
 *
 * Es inyeccion de dependencias hecha a mano: las clases no salen a buscar lo que necesitan, lo
 * reciben por constructor. Eso permite, por ejemplo, pasarle a un repositorio una base de datos en
 * memoria durante las pruebas cambiando una sola linea de este archivo.
 */
public class ServiceLocator {

    private final Context context;

    private SplitBillDatabase database;
    private AppExecutors executors;
    private UserRepository userRepository;
    private ExpenseRepository expenseRepository;
    private SettlementRepository settlementRepository;

    public ServiceLocator(Context context) {
        this.context = context.getApplicationContext();
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

    public synchronized UserRepository getUserRepository() {
        if (this.userRepository == null) {
            this.userRepository = new UserRepository(getDatabase(), getExecutors());
        }
        return this.userRepository;
    }

    public synchronized ExpenseRepository getExpenseRepository() {
        if (this.expenseRepository == null) {
            this.expenseRepository = new ExpenseRepository(getDatabase(), getExecutors());
        }
        return this.expenseRepository;
    }

    public synchronized SettlementRepository getSettlementRepository() {
        if (this.settlementRepository == null) {
            this.settlementRepository = new SettlementRepository(
                    getDatabase(),
                    getExecutors(),
                    new BalanceCalculator(),
                    new DebtSimplifier());
        }
        return this.settlementRepository;
    }
}
