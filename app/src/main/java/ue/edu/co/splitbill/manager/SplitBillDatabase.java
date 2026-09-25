package ue.edu.co.splitbill.manager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import ue.edu.co.splitbill.dao.BalanceDao;
import ue.edu.co.splitbill.dao.ExpenseDao;
import ue.edu.co.splitbill.dao.ExpenseShareDao;
import ue.edu.co.splitbill.dao.GroupDao;
import ue.edu.co.splitbill.dao.GroupMemberDao;
import ue.edu.co.splitbill.dao.QuickSplitDao;
import ue.edu.co.splitbill.dao.UserDao;
import ue.edu.co.splitbill.entity.Expense;
import ue.edu.co.splitbill.entity.ExpenseShare;
import ue.edu.co.splitbill.entity.Group;
import ue.edu.co.splitbill.entity.GroupMember;
import ue.edu.co.splitbill.entity.QuickSplit;
import ue.edu.co.splitbill.entity.QuickSplitShare;
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
        entities = {User.class, Group.class, Expense.class, ExpenseShare.class, GroupMember.class,
                QuickSplit.class, QuickSplitShare.class},
        version = DatabaseContract.DATABASE_VERSION,
        exportSchema = true)
@TypeConverters({Converters.class})
public abstract class SplitBillDatabase extends RoomDatabase {

    private static volatile SplitBillDatabase instance;

    public abstract UserDao userDao();

    public abstract ExpenseDao expenseDao();

    public abstract ExpenseShareDao expenseShareDao();

    public abstract BalanceDao balanceDao();

    public abstract GroupDao groupDao();

    public abstract GroupMemberDao groupMemberDao();

    public abstract QuickSplitDao quickSplitDao();

    /**
     * Version 1 -> 2 (entrega 3): la tabla groups necesita saber si ya se subio al servidor y quien
     * es su dueno. ALTER TABLE agrega las columnas sin tocar las filas existentes, asi que los gastos
     * registrados antes de actualizar la app se conservan. El grupo que ya existia queda pendiente de
     * crear en el servidor.
     */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `" + DatabaseContract.Groups.TABLE_NAME + "` ADD COLUMN "
                    + DatabaseContract.Groups.COLUMN_SYNC_STATUS + " TEXT");
            database.execSQL("ALTER TABLE `" + DatabaseContract.Groups.TABLE_NAME + "` ADD COLUMN "
                    + DatabaseContract.Groups.COLUMN_OWNER_ID + " TEXT");
            database.execSQL("UPDATE `" + DatabaseContract.Groups.TABLE_NAME + "` SET "
                    + DatabaseContract.Groups.COLUMN_SYNC_STATUS + " = " + DatabaseContract.PENDING_CREATE);
        }
    };

    /**
     * Version 2 -> 3 (entrega 4): varios grupos. Nace la tabla group_members y se llena con lo que
     * habia: antes de esta version el celular tenia un solo grupo, asi que cada persona de users
     * pertenece a el, con el mismo estado (activa o retirada) y la misma marca de sincronizacion.
     */
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `group_members` (`gmb_group_id` TEXT NOT NULL, "
                    + "`gmb_user_id` TEXT NOT NULL, `gmb_status` INTEGER NOT NULL, `gmb_sync_status` TEXT, "
                    + "PRIMARY KEY(`gmb_group_id`, `gmb_user_id`), "
                    + "FOREIGN KEY(`gmb_group_id`) REFERENCES `groups`(`grp_id`) ON UPDATE NO ACTION ON DELETE CASCADE , "
                    + "FOREIGN KEY(`gmb_user_id`) REFERENCES `users`(`use_id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_group_members_gmb_user_id` "
                    + "ON `group_members` (`gmb_user_id`)");
            database.execSQL("INSERT INTO group_members (gmb_group_id, gmb_user_id, gmb_status, gmb_sync_status) "
                    + "SELECT g.grp_id, u.use_id, u.use_status, u.use_sync_status "
                    + "FROM `groups` g CROSS JOIN users u WHERE g.grp_status = 1");
        }
    };

    /**
     * Version 3 -> 4 (rediseno): cada gasto tiene categoria. Los que ya existian quedan como OTHER;
     * el DEFAULT es el mismo que declara la entidad, asi Room reconoce el esquema como valido.
     */
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `" + DatabaseContract.Expenses.TABLE_NAME + "` ADD COLUMN `"
                    + DatabaseContract.Expenses.COLUMN_CATEGORY + "` TEXT NOT NULL DEFAULT 'OTHER'");
        }
    };

    private static final String CREATE_QUICK_SPLITS =
            "CREATE TABLE IF NOT EXISTS `quick_splits` (`qsp_id` TEXT NOT NULL, `qsp_description` TEXT, "
            + "`qsp_subtotal_cents` INTEGER NOT NULL, `qsp_tip_percent` TEXT, `qsp_total_cents` INTEGER NOT NULL, "
            + "`qsp_split_type` TEXT, `qsp_date` INTEGER, `qsp_status` INTEGER NOT NULL, `qsp_sync_status` TEXT, "
            + "PRIMARY KEY(`qsp_id`))";

    private static final String CREATE_QUICK_SPLIT_SHARES =
            "CREATE TABLE IF NOT EXISTS `quick_split_shares` (`qss_id` TEXT NOT NULL, "
            + "`qss_quick_split_id` TEXT NOT NULL, `qss_position` INTEGER NOT NULL, `qss_name` TEXT, "
            + "`qss_amount_cents` INTEGER NOT NULL, PRIMARY KEY(`qss_id`), "
            + "FOREIGN KEY(`qss_quick_split_id`) REFERENCES `quick_splits`(`qsp_id`) "
            + "ON UPDATE NO ACTION ON DELETE CASCADE )";

    private static final String CREATE_QUICK_SPLIT_SHARES_INDEX =
            "CREATE INDEX IF NOT EXISTS `index_quick_split_shares_qss_quick_split_id` "
            + "ON `quick_split_shares` (`qss_quick_split_id`)";

    /**
     * Version 4 -> 5: cuentas rapidas guardadas. Son tablas nuevas, asi que no se toca ningun dato.
     * El SQL es el mismo que Room genera para las entidades (app/schemas/.../5.json).
     */
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(CREATE_QUICK_SPLITS);
            database.execSQL(CREATE_QUICK_SPLIT_SHARES);
            database.execSQL(CREATE_QUICK_SPLIT_SHARES_INDEX);
        }
    };

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
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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
                            + DatabaseContract.Groups.COLUMN_STATUS + ", "
                            + DatabaseContract.Groups.COLUMN_SYNC_STATUS
                            + ") VALUES (?, ?, ?, ?, ?, ?)",
                    new Object[]{
                            DatabaseContract.DEFAULT_GROUP_ID,
                            DatabaseContract.DEFAULT_GROUP_NAME,
                            DatabaseContract.DEFAULT_GROUP_CURRENCY,
                            System.currentTimeMillis(),
                            DatabaseContract.STATUS_ACTIVE,
                            "PENDING_CREATE"
                    });
        }
    };
}
