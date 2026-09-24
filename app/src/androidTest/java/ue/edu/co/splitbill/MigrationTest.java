package ue.edu.co.splitbill;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import ue.edu.co.splitbill.manager.DatabaseContract;
import ue.edu.co.splitbill.manager.SplitBillDatabase;

/**
 * Prueba de la migracion 1 -> 2 sobre un archivo de base de datos real.
 *
 * Se crea la base con el esquema exacto de la entrega 1 (schemas/1.json), se le meten datos como los
 * que tendria un usuario, se ejecuta la migracion y Room verifica que el resultado sea identico al
 * esquema 2 (schemas/2.json). Si la migracion olvidara una columna o la creara con otro tipo, falla.
 */
@RunWith(AndroidJUnit4.class)
public class MigrationTest {

    private static final String TEST_DB = "migration-test.db";
    private static final String USER_ID = "u-1";
    private static final String EXPENSE_ID = "e-1";

    @Rule
    public MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), SplitBillDatabase.class);

    @Test
    public void migrationFrom1To2KeepsTheExpensesAndLeavesTheGroupPendingToUpload() throws IOException {
        SupportSQLiteDatabase version1 = helper.createDatabase(TEST_DB, 1);
        version1.execSQL("INSERT INTO `groups` (grp_id, grp_name, grp_currency, grp_created_at, grp_status) "
                + "VALUES (?, 'Mi grupo', 'COP', 0, 1)", new Object[]{DatabaseContract.DEFAULT_GROUP_ID});
        version1.execSQL("INSERT INTO users (use_id, use_names, use_status, use_sync_status) "
                + "VALUES (?, 'Julian', 1, 'PENDING_CREATE')", new Object[]{USER_ID});
        version1.execSQL("INSERT INTO expenses (exp_id, exp_group_id, exp_payer_id, exp_description, "
                        + "exp_amount_cents, exp_split_type, exp_date, exp_status, exp_sync_status) "
                        + "VALUES (?, ?, ?, 'Almuerzo', 6000000, 'EQUAL', 0, 1, 'PENDING_CREATE')",
                new Object[]{EXPENSE_ID, DatabaseContract.DEFAULT_GROUP_ID, USER_ID});
        version1.close();

        SupportSQLiteDatabase version2 = helper.runMigrationsAndValidate(TEST_DB, 2, true,
                SplitBillDatabase.MIGRATION_1_2);

        try (Cursor group = version2.query("SELECT grp_sync_status, grp_owner_id FROM `groups`")) {
            assertTrue(group.moveToFirst());
            assertEquals("PENDING_CREATE", group.getString(0));
            assertTrue(group.isNull(1));
        }
        try (Cursor expense = version2.query("SELECT exp_description, exp_amount_cents FROM expenses")) {
            assertTrue(expense.moveToFirst());
            assertEquals("Almuerzo", expense.getString(0));
            assertEquals(6_000_000L, expense.getLong(1));
        }
        version2.close();
    }

    /**
     * 2 -> 3: el celular tenia un solo grupo; cada persona queda como integrante de el, con el mismo
     * estado. Una retirada sigue retirada y una sin subir sigue pendiente.
     */
    @Test
    public void migrationFrom2To3PutsEveryPersonInTheGroupTheyWereIn() throws IOException {
        SupportSQLiteDatabase version2 = helper.createDatabase(TEST_DB, 2);
        version2.execSQL("INSERT INTO `groups` (grp_id, grp_name, grp_currency, grp_created_at, grp_status, "
                + "grp_sync_status) VALUES ('g-1', 'Paseo', 'COP', 0, 1, 'SYNCED')");
        version2.execSQL("INSERT INTO users (use_id, use_names, use_status, use_sync_status) "
                + "VALUES ('u-1', 'Julian', 1, 'SYNCED')");
        version2.execSQL("INSERT INTO users (use_id, use_names, use_status, use_sync_status) "
                + "VALUES ('u-2', 'Diomar', 1, 'PENDING_CREATE')");
        version2.execSQL("INSERT INTO users (use_id, use_names, use_status, use_sync_status) "
                + "VALUES ('u-3', 'Luis', 0, 'SYNCED')");
        version2.close();

        SupportSQLiteDatabase version3 = helper.runMigrationsAndValidate(TEST_DB, 3, true,
                SplitBillDatabase.MIGRATION_2_3);

        try (Cursor members = version3.query("SELECT gmb_user_id, gmb_status, gmb_sync_status FROM group_members "
                + "WHERE gmb_group_id = 'g-1' ORDER BY gmb_user_id")) {
            assertEquals(3, members.getCount());
            members.moveToPosition(1);
            assertEquals("u-2", members.getString(0));
            assertEquals("PENDING_CREATE", members.getString(2));
            members.moveToPosition(2);
            assertEquals("u-3", members.getString(0));
            assertEquals(0, members.getInt(1));
        }
        version3.close();
    }
}
