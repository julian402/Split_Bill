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
 * Pruebas de las migraciones (1 -> 2, 2 -> 3, 3 -> 4, 4 -> 5 y 5 -> 6) sobre un archivo de base de datos real.
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

    /** 3 -> 4: los gastos que ya existian quedan con categoria OTHER y no se pierde ninguno. */
    @Test
    public void migrationFrom3To4LeavesOldExpensesAsOther() throws IOException {
        SupportSQLiteDatabase version3 = helper.createDatabase(TEST_DB, 3);
        version3.execSQL("INSERT INTO `groups` (grp_id, grp_name, grp_currency, grp_created_at, grp_status, "
                + "grp_sync_status) VALUES ('g-1', 'Paseo', 'COP', 0, 1, 'SYNCED')");
        version3.execSQL("INSERT INTO users (use_id, use_names, use_status, use_sync_status) "
                + "VALUES ('u-1', 'Julian', 1, 'SYNCED')");
        version3.execSQL("INSERT INTO expenses (exp_id, exp_group_id, exp_payer_id, exp_description, "
                + "exp_amount_cents, exp_split_type, exp_date, exp_status, exp_sync_status) "
                + "VALUES ('e-1', 'g-1', 'u-1', 'Almuerzo', 6000000, 'EQUAL', 0, 1, 'SYNCED')");
        version3.close();

        SupportSQLiteDatabase version4 = helper.runMigrationsAndValidate(TEST_DB, 4, true,
                SplitBillDatabase.MIGRATION_3_4);

        try (Cursor expense = version4.query("SELECT exp_description, exp_category FROM expenses")) {
            assertTrue(expense.moveToFirst());
            assertEquals("Almuerzo", expense.getString(0));
            assertEquals("OTHER", expense.getString(1));
        }
        version4.close();
    }

    /** 4 -> 5: nacen las tablas de cuentas rapidas, vacias, y los gastos siguen ahi. */
    @Test
    public void migrationFrom4To5AddsTheQuickSplitTablesAndKeepsTheExpenses() throws IOException {
        SupportSQLiteDatabase version4 = helper.createDatabase(TEST_DB, 4);
        version4.execSQL("INSERT INTO `groups` (grp_id, grp_name, grp_currency, grp_created_at, grp_status, "
                + "grp_sync_status) VALUES ('g-1', 'Paseo', 'COP', 0, 1, 'SYNCED')");
        version4.execSQL("INSERT INTO users (use_id, use_names, use_status, use_sync_status) "
                + "VALUES ('u-1', 'Julian', 1, 'SYNCED')");
        version4.execSQL("INSERT INTO expenses (exp_id, exp_group_id, exp_payer_id, exp_description, "
                + "exp_amount_cents, exp_split_type, exp_date, exp_category, exp_status, exp_sync_status) "
                + "VALUES ('e-1', 'g-1', 'u-1', 'Almuerzo', 6000000, 'EQUAL', 0, 'FOOD', 1, 'SYNCED')");
        version4.close();

        SupportSQLiteDatabase version5 = helper.runMigrationsAndValidate(TEST_DB, 5, true,
                SplitBillDatabase.MIGRATION_4_5);

        try (Cursor expenses = version5.query("SELECT COUNT(*) FROM expenses")) {
            assertTrue(expenses.moveToFirst());
            assertEquals(1, expenses.getInt(0));
        }
        try (Cursor quickSplits = version5.query("SELECT COUNT(*) FROM quick_splits")) {
            assertTrue(quickSplits.moveToFirst());
            assertEquals(0, quickSplits.getInt(0));
        }
        version5.close();
    }

    /** 5 -> 6: nace la tabla del chat, vacia, y las cuentas rapidas guardadas siguen ahi. */
    @Test
    public void migrationFrom5To6AddsTheChatAndKeepsTheData() throws IOException {
        SupportSQLiteDatabase version5 = helper.createDatabase(TEST_DB, 5);
        version5.execSQL("INSERT INTO quick_splits (qsp_id, qsp_description, qsp_subtotal_cents, qsp_tip_percent, "
                + "qsp_total_cents, qsp_split_type, qsp_date, qsp_status, qsp_sync_status) "
                + "VALUES ('q-1', 'Cena', 10000000, '10', 11000000, 'EQUAL', 0, 1, 'SYNCED')");
        version5.close();

        SupportSQLiteDatabase version6 = helper.runMigrationsAndValidate(TEST_DB, 6, true,
                SplitBillDatabase.MIGRATION_5_6);

        try (Cursor quickSplits = version6.query("SELECT COUNT(*) FROM quick_splits")) {
            assertTrue(quickSplits.moveToFirst());
            assertEquals(1, quickSplits.getInt(0));
        }
        try (Cursor messages = version6.query("SELECT COUNT(*) FROM messages")) {
            assertTrue(messages.moveToFirst());
            assertEquals(0, messages.getInt(0));
        }
        version6.close();
    }
}
