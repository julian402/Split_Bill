package ue.edu.co.splitbill.manager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import ue.edu.co.splitbill.dao.BalanceDao;
import ue.edu.co.splitbill.dao.ExpenseDao;
import ue.edu.co.splitbill.dao.ExpenseShareDao;
import ue.edu.co.splitbill.dao.UserDao;
import ue.edu.co.splitbill.entity.Expense;
import ue.edu.co.splitbill.entity.ExpenseShare;
import ue.edu.co.splitbill.entity.Group;
import ue.edu.co.splitbill.entity.User;

/**
 * Base de datos SQLite de la aplicacion.
 *
 * Cumple el mismo papel que un SQLiteOpenHelper: concentra el nombre del archivo, la version del
 * esquema y la creacion de las tablas. La diferencia es que Room genera el CREATE TABLE a partir de
 * las entidades y verifica en tiempo de compilacion que cada @Query sea SQL valido contra ese
 * esquema, de modo que un nombre de columna mal escrito rompe el build y no la aplicacion.
 *
 * Es un singleton: abrir la base de datos es costoso y varias conexiones sobre el mismo archivo
 * generan bloqueos. Se usa doble verificacion con synchronized para que dos hilos no la abran a la vez.
 */
@Database(
        entities = {User.class, Group.class, Expense.class, ExpenseShare.class},
        version = DatabaseContract.DATABASE_VERSION,
        exportSchema = true)
@TypeConverters({Converters.class})
public abstract class SplitBillDatabase extends RoomDatabase {

    private static volatile SplitBillDatabase instance;

    public abstract UserDao userDao();

    public abstract ExpenseDao expenseDao();

    public abstract ExpenseShareDao expenseShareDao();

    public abstract BalanceDao balanceDao();

    public static SplitBillDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (SplitBillDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    SplitBillDatabase.class,
                                    DatabaseContract.DATABASE_NAME)
                            //Sin fallbackToDestructiveMigration: perder datos del usuario al cambiar
                            //el esquema no es una opcion, las migraciones se escriben a mano
                            .addCallback(CALLBACK)
                            .build();
                }
            }
        }
        return instance;
    }

    /**
     * Siembra el grupo por defecto la primera vez que se crea la base de datos.
     * En esta entrega la aplicacion trabaja siempre sobre ese grupo.
     */
    private static final RoomDatabase.Callback CALLBACK = new RoomDatabase.Callback() {

        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase database) {
            super.onCreate(database);
            database.execSQL(
                    "INSERT INTO `" + DatabaseContract.Groups.TABLE_NAME + "` ("
                            + DatabaseContract.Groups.COLUMN_ID + ", "
                            + DatabaseContract.Groups.COLUMN_NAME + ", "
                            + DatabaseContract.Groups.COLUMN_CURRENCY + ", "
                            + DatabaseContract.Groups.COLUMN_CREATED_AT + ", "
                            + DatabaseContract.Groups.COLUMN_STATUS
                            + ") VALUES (?, ?, ?, ?, ?)",
                    new Object[]{
                            DatabaseContract.DEFAULT_GROUP_ID,
                            DatabaseContract.DEFAULT_GROUP_NAME,
                            DatabaseContract.DEFAULT_GROUP_CURRENCY,
                            System.currentTimeMillis(),
                            DatabaseContract.STATUS_ACTIVE
                    });
        }
    };
}
