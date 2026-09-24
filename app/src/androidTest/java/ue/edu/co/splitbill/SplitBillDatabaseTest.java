package ue.edu.co.splitbill;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ue.edu.co.splitbill.dao.ActivityItem;
import ue.edu.co.splitbill.dao.GroupListItem;
import ue.edu.co.splitbill.dao.ExpenseListItem;
import ue.edu.co.splitbill.dao.ShareListItem;
import ue.edu.co.splitbill.dao.UserAmount;
import ue.edu.co.splitbill.domain.Balance;
import ue.edu.co.splitbill.domain.BalanceCalculator;
import ue.edu.co.splitbill.domain.DebtSimplifier;
import ue.edu.co.splitbill.domain.ExpenseCategory;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.domain.Share;
import ue.edu.co.splitbill.domain.SplitType;
import ue.edu.co.splitbill.domain.Transfer;
import ue.edu.co.splitbill.domain.split.SplitRequest;
import ue.edu.co.splitbill.domain.split.SplitStrategyFactory;
import ue.edu.co.splitbill.entity.Expense;
import ue.edu.co.splitbill.entity.ExpenseShare;
import ue.edu.co.splitbill.entity.Group;
import ue.edu.co.splitbill.entity.GroupMember;
import ue.edu.co.splitbill.entity.SyncStatus;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.manager.DatabaseContract;
import ue.edu.co.splitbill.manager.SplitBillDatabase;

/**
 * Pruebas de la capa de persistencia sobre una base de datos real de SQLite.
 *
 * Las pruebas del dominio corren sin dispositivo porque son Java puro, pero el SQL de los @Dao solo
 * se puede verificar contra SQLite de verdad. Aqui se comprueba que el JOIN de la lista de gastos y
 * las dos consultas de agregacion con GROUP BY devuelven lo que espera BalanceCalculator.
 *
 * La base de datos se crea en memoria, asi que cada prueba arranca limpia y no deja rastro.
 */
@RunWith(AndroidJUnit4.class)
public class SplitBillDatabaseTest {

    private SplitBillDatabase database;
    private String groupId;
    private User julian;
    private User diomar;
    private User juan;
    private User sofia;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        this.database = Room.inMemoryDatabaseBuilder(context, SplitBillDatabase.class)
                .allowMainThreadQueries()
                .build();

        //La base en memoria no ejecuta el callback que siembra el grupo, asi que se crea aqui
        Group group = new Group(DatabaseContract.DEFAULT_GROUP_NAME, DatabaseContract.DEFAULT_GROUP_CURRENCY);
        this.groupId = group.getId();
        this.database.getOpenHelper().getWritableDatabase().execSQL(
                "INSERT INTO `groups` (grp_id, grp_name, grp_currency, grp_created_at, grp_status) "
                        + "VALUES (?, ?, ?, ?, ?)",
                new Object[]{group.getId(), group.getName(), group.getCurrency(),
                        group.getCreatedAt().getTime(), DatabaseContract.STATUS_ACTIVE});

        this.julian = insertUser("Julian Corredor");
        this.diomar = insertUser("Diomar Arias");
        this.juan = insertUser("Juan Rojano");
        this.sofia = insertUser("Sofia Reyes");
    }

    @After
    public void tearDown() {
        this.database.close();
    }

    @Test
    public void losIntegrantesActivosSeListanPorNombre() {
        List<User> users = this.database.groupMemberDao().findActiveUsers(this.groupId);

        assertEquals(4, users.size());
        assertEquals("Diomar Arias", users.get(0).getNames());
        assertEquals(4, this.database.groupMemberDao().countActive(this.groupId));
    }

    @Test
    public void elBorradoLogicoSacaAlIntegranteDeLaLista() {
        assertEquals(1, this.database.groupMemberDao().softDelete(this.groupId, this.sofia.getId()));

        assertEquals(3, this.database.groupMemberDao().countActive(this.groupId));
        //La fila sigue existiendo: solo quedo marcada como inactiva
        assertNotNull(this.database.userDao().findById(this.sofia.getId()));
    }

    /** Una misma persona puede estar en dos grupos y salir de uno sin salir del otro. */
    @Test
    public void cadaGrupoTieneSusPropiosIntegrantes() {
        Group casa = new Group("Casa", DatabaseContract.DEFAULT_GROUP_CURRENCY);
        this.database.groupDao().insert(casa);
        this.database.groupMemberDao().upsert(new GroupMember(casa.getId(), this.julian.getId(), SyncStatus.SYNCED));
        this.database.groupMemberDao().upsert(new GroupMember(casa.getId(), this.sofia.getId(), SyncStatus.SYNCED));

        assertEquals(2, this.database.groupMemberDao().countActive(casa.getId()));
        assertEquals(4, this.database.groupMemberDao().countActive(this.groupId));

        this.database.groupMemberDao().softDelete(casa.getId(), this.sofia.getId());
        assertEquals(1, this.database.groupMemberDao().countActive(casa.getId()));
        assertEquals(4, this.database.groupMemberDao().countActive(this.groupId));
        //el retiro queda pendiente de subir al servidor
        assertEquals(SyncStatus.PENDING_DELETE,
                this.database.groupMemberDao().findById(casa.getId(), this.sofia.getId()).getSyncStatus());

        List<GroupListItem> groups = this.database.groupDao().findActiveWithTotals(this.julian.getId());
        assertEquals(2, groups.size());
    }

    @Test
    public void laListaDeGastosTraeElNombreDeQuienPago() {
        insertExpense("Almuerzo", "60000", this.julian, SplitType.EQUAL, null);

        List<ExpenseListItem> items = this.database.expenseDao().findActiveWithPayer(this.groupId);

        assertEquals(1, items.size());
        assertEquals("Almuerzo", items.get(0).getDescription());
        assertEquals("Julian Corredor", items.get(0).getPayerNames());
        assertEquals(Money.of("60000"), items.get(0).getAmount());
        assertEquals(SplitType.EQUAL, items.get(0).getSplitType());
    }

    @Test
    public void elTotalDelGrupoLoSumaLaBaseDeDatos() {
        insertExpense("Almuerzo", "60000", this.julian, SplitType.EQUAL, null);
        insertExpense("Gasolina", "80000", this.diomar, SplitType.EQUAL, null);

        assertEquals(Money.of("140000"),
                Money.ofCents(this.database.expenseDao().sumActiveCents(this.groupId)));
    }

    @Test
    public void elGastoBorradoDejaDeContar() {
        String expenseId = insertExpense("Almuerzo", "60000", this.julian, SplitType.EQUAL, null);
        insertExpense("Gasolina", "80000", this.diomar, SplitType.EQUAL, null);

        assertEquals(1, this.database.expenseDao().softDelete(expenseId));

        assertEquals(1, this.database.expenseDao().findActiveWithPayer(this.groupId).size());
        assertEquals(Money.of("80000"),
                Money.ofCents(this.database.expenseDao().sumActiveCents(this.groupId)));
        //Las partes del gasto borrado tampoco cuentan en los saldos
        assertEquals(Money.of("80000"), sumar(this.database.balanceDao().sumOwedByUser(this.groupId)));
    }

    /** La pantalla de detalle muestra cada parte con el nombre del participante, de mayor a menor. */
    @Test
    public void elDetalleTraeLasPartesConElNombreDeCadaParticipante() {
        String expenseId = insertExpense("Mercado", "40000", this.juan, SplitType.PERCENTAGE, porcentajes());

        List<ShareListItem> shares = this.database.expenseShareDao().findByExpenseWithNames(expenseId);

        assertEquals(4, shares.size());
        assertEquals("Julian Corredor", shares.get(0).getNames());
        assertEquals(Money.of("16000"), shares.get(0).getAmount());
        assertEquals("Sofia Reyes", shares.get(3).getNames());
        assertEquals(Money.of("4000"), shares.get(3).getAmount());
        long total = 0;
        for (ShareListItem share : shares) {
            total += share.getAmountCents();
        }
        //las partes suman exactamente el gasto
        assertEquals(Money.of("40000"), Money.ofCents(total));
    }

    /**
     * Recorre el escenario completo de la sustentacion contra SQLite: tres gastos con dos formas de
     * division distintas, los saldos calculados con SUM y GROUP BY, y la liquidacion final.
     */
    @Test
    public void tresGastosSeLiquidanConTresTransferencias() {
        insertExpense("Almuerzo", "60000", this.julian, SplitType.EQUAL, null);
        insertExpense("Gasolina", "80000", this.diomar, SplitType.EQUAL, null);
        insertExpense("Mercado", "40000", this.juan, SplitType.PERCENTAGE, porcentajes());

        Map<String, Money> paid = aMapa(this.database.balanceDao().sumPaidByUser(this.groupId));
        Map<String, Money> owed = aMapa(this.database.balanceDao().sumOwedByUser(this.groupId));

        //Lo que se pago y lo que se debia pagar tienen que coincidir con el total gastado
        assertEquals(Money.of("180000"), sumar(this.database.balanceDao().sumPaidByUser(this.groupId)));
        assertEquals(Money.of("180000"), sumar(this.database.balanceDao().sumOwedByUser(this.groupId)));

        List<Balance> balances = new BalanceCalculator().calcularBalances(idsDelGrupo(), paid, owed);
        assertEquals(Money.of("9000"), buscar(balances, this.julian).getAmount());
        assertEquals(Money.of("33000"), buscar(balances, this.diomar).getAmount());
        assertEquals(Money.of("-3000"), buscar(balances, this.juan).getAmount());
        assertEquals(Money.of("-39000"), buscar(balances, this.sofia).getAmount());

        List<Transfer> transfers = new DebtSimplifier().simplificar(balances);
        assertTrue("Se generaron " + transfers.size() + " transferencias", transfers.size() <= 3);
    }

    /**
     * "Marcar como pagado": el pago de Diomar a Julian no suma al total gastado del grupo, pero si
     * entra en los saldos y deja a Diomar en cero. En la lista se ve a quien se le pago.
     */
    @Test
    public void unPagoNoCuentaComoGastoPeroSaldaLaDeuda() {
        insertExpense("Almuerzo", "60000", this.julian, SplitType.EQUAL, null);
        Expense payment = new Expense(this.groupId, this.diomar.getId(), "Pago a Julian",
                Money.of("15000"), SplitType.EXACT);
        payment.setCategory(ExpenseCategory.PAYMENT);
        this.database.expenseDao().insert(payment);
        this.database.expenseShareDao().insertAll(Arrays.asList(
                new ExpenseShare(payment.getId(), new Share(this.julian.getId(), Money.of("15000")))));

        assertEquals(Money.of("60000"), Money.ofCents(this.database.expenseDao().sumActiveCents(this.groupId)));
        assertEquals(3, this.database.balanceDao().countDirectTransfers(this.groupId));

        List<Balance> balances = new BalanceCalculator().calcularBalances(idsDelGrupo(),
                aMapa(this.database.balanceDao().sumPaidByUser(this.groupId)),
                aMapa(this.database.balanceDao().sumOwedByUser(this.groupId)));
        assertTrue(buscar(balances, this.diomar).getAmount().isZero());
        assertEquals(Money.of("30000"), buscar(balances, this.julian).getAmount());

        ExpenseListItem item = null;
        for (ExpenseListItem candidate : this.database.expenseDao().findActiveWithPayer(this.groupId)) {
            if (candidate.isPayment()) {
                item = candidate;
            }
        }
        assertNotNull(item);
        assertEquals("Diomar Arias", item.getPayerNames());
        assertEquals("Julian Corredor", item.getPayeeNames());
    }

    /**
     * Inicio: el saldo de la persona en cada grupo, lo que le toco a ella y la actividad de todos los
     * grupos con el nombre del grupo.
     */
    @Test
    public void elInicioResumeLosGastosDeTodosLosGrupos() {
        insertExpense("Almuerzo", "60000", this.julian, SplitType.EQUAL, null);
        insertExpense("Gasolina", "80000", this.diomar, SplitType.EQUAL, null);

        List<GroupListItem> groups = this.database.groupDao().findActiveWithTotals(this.julian.getId());
        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).getExpenseCount());
        assertEquals(Money.of("140000"), groups.get(0).getTotal());
        //Julian pago 60.000 y le tocaban 15.000 + 20.000
        assertEquals(Money.of("25000"), groups.get(0).getBalance());

        assertEquals(Money.of("140000"), Money.ofCents(this.database.expenseDao().sumAllGroupsSince(0L)));
        assertEquals(Money.of("35000"),
                Money.ofCents(this.database.expenseDao().sumUserSharesAllGroups(this.julian.getId())));

        List<ActivityItem> recent = this.database.expenseDao().findRecentAllGroups(1);
        assertEquals(1, recent.size());
        assertEquals(DatabaseContract.DEFAULT_GROUP_NAME, recent.get(0).getGroupName());
    }

    //Metodos de apoyo

    private User insertUser(String names) {
        User user = new User(names, null, null);
        this.database.userDao().insert(user);
        this.database.groupMemberDao().upsert(new GroupMember(this.groupId, user.getId(), SyncStatus.PENDING_CREATE));
        return user;
    }

    private String insertExpense(String description, String amount, User payer,
                                 SplitType splitType, Map<String, BigDecimal> values) {
        Expense expense = new Expense(this.groupId, payer.getId(), description,
                Money.of(amount), splitType);

        SplitRequest request = values == null
                ? new SplitRequest(Money.of(amount), idsDelGrupo())
                : new SplitRequest(Money.of(amount), idsDelGrupo(), values);

        List<Share> shares = SplitStrategyFactory.create(splitType).split(request);
        List<ExpenseShare> expenseShares = new ArrayList<>(shares.size());
        for (Share share : shares) {
            expenseShares.add(new ExpenseShare(expense.getId(), share));
        }

        this.database.expenseDao().insert(expense);
        this.database.expenseShareDao().insertAll(expenseShares);
        return expense.getId();
    }

    private List<String> idsDelGrupo() {
        return Arrays.asList(this.julian.getId(), this.diomar.getId(),
                this.juan.getId(), this.sofia.getId());
    }

    private Map<String, BigDecimal> porcentajes() {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put(this.julian.getId(), new BigDecimal("40"));
        values.put(this.diomar.getId(), new BigDecimal("30"));
        values.put(this.juan.getId(), new BigDecimal("20"));
        values.put(this.sofia.getId(), new BigDecimal("10"));
        return values;
    }

    private Map<String, Money> aMapa(List<UserAmount> amounts) {
        Map<String, Money> result = new LinkedHashMap<>();
        for (UserAmount amount : amounts) {
            result.put(amount.getUserId(), amount.getTotal());
        }
        return result;
    }

    private Money sumar(List<UserAmount> amounts) {
        Money sum = Money.ZERO;
        for (UserAmount amount : amounts) {
            sum = sum.plus(amount.getTotal());
        }
        return sum;
    }

    private Balance buscar(List<Balance> balances, User user) {
        for (Balance balance : balances) {
            if (balance.getUserId().equals(user.getId())) {
                return balance;
            }
        }
        throw new AssertionError("No se encontró el saldo de " + user.getNames());
    }
}
