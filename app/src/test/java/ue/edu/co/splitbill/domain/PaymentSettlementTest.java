package ue.edu.co.splitbill.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Marcar como pagado": un pago se registra como un gasto que paga el deudor y cuya unica parte es
 * del acreedor. Estas pruebas comprueban que, sumado como cualquier gasto, deja a todos en cero.
 */
public class PaymentSettlementTest {

    private static final List<String> MEMBERS = Arrays.asList("andres", "luis", "maria");

    private BalanceCalculator calculator;
    private DebtSimplifier simplifier;
    private Map<String, Money> paid;
    private Map<String, Money> owed;

    @Before
    public void setUp() {
        this.calculator = new BalanceCalculator();
        this.simplifier = new DebtSimplifier();
        //Andres pago la gasolina de $90.000 entre los tres
        this.paid = new LinkedHashMap<>();
        this.paid.put("andres", Money.of("90000"));
        this.owed = new LinkedHashMap<>();
        this.owed.put("andres", Money.of("30000"));
        this.owed.put("luis", Money.of("30000"));
        this.owed.put("maria", Money.of("30000"));
    }

    @Test
    public void registrarTodasLasTransferenciasDejaATodosEnCero() {
        List<Transfer> transfers = this.simplifier.simplificar(
                this.calculator.calcularBalances(MEMBERS, this.paid, this.owed));
        assertEquals(2, transfers.size());

        for (Transfer transfer : transfers) {
            registrarPago(transfer);
        }

        List<Balance> after = this.calculator.calcularBalances(MEMBERS, this.paid, this.owed);
        for (Balance balance : after) {
            assertTrue(balance.getUserId() + " deberia quedar en cero", balance.getAmount().isZero());
        }
        assertTrue(this.simplifier.simplificar(after).isEmpty());
    }

    @Test
    public void unPagoParcialSoloSaldaAQuienPago() {
        List<Transfer> transfers = this.simplifier.simplificar(
                this.calculator.calcularBalances(MEMBERS, this.paid, this.owed));

        registrarPago(transfers.get(0));

        List<Transfer> remaining = this.simplifier.simplificar(
                this.calculator.calcularBalances(MEMBERS, this.paid, this.owed));
        assertEquals(1, remaining.size());
        assertEquals(transfers.get(1), remaining.get(0));
        assertFalse(remaining.contains(transfers.get(0)));
    }

    @Test
    public void laCategoriaDePagoNoSePuedeEscogerAlRegistrarUnGasto() {
        assertFalse(ExpenseCategory.selectable().contains(ExpenseCategory.PAYMENT));
        assertEquals(ExpenseCategory.FOOD, ExpenseCategory.fromPosition(0));
        assertEquals(ExpenseCategory.OTHER, ExpenseCategory.fromName("DESCONOCIDA"));
        assertEquals(ExpenseCategory.OTHER, ExpenseCategory.fromName(null));
        assertEquals(ExpenseCategory.PAYMENT, ExpenseCategory.fromName("PAYMENT"));
        for (ExpenseCategory category : ExpenseCategory.selectable()) {
            assertEquals(category, ExpenseCategory.fromPosition(category.getPosition()));
        }
    }

    /** Lo mismo que guarda SettlementRepository.markPaid: el deudor pone el monto y es todo del acreedor. */
    private void registrarPago(Transfer transfer) {
        sumar(this.paid, transfer.getFromUserId(), transfer.getAmount());
        sumar(this.owed, transfer.getToUserId(), transfer.getAmount());
    }

    private static void sumar(Map<String, Money> totals, String userId, Money amount) {
        Money current = totals.get(userId);
        totals.put(userId, current == null ? amount : current.plus(amount));
    }
}
