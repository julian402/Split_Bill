package ue.edu.co.splitbill.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BalanceCalculatorTest {

    private BalanceCalculator calculator;

    @Before
    public void setUp() {
        this.calculator = new BalanceCalculator();
    }

    @Test
    public void elSaldoEsLoPagadoMenosLoAdeudado() {
        Map<String, Money> paid = new LinkedHashMap<>();
        paid.put("julian", Money.of("60000"));

        Map<String, Money> owed = new LinkedHashMap<>();
        owed.put("julian", Money.of("15000"));
        owed.put("diomar", Money.of("15000"));
        owed.put("juan", Money.of("15000"));
        owed.put("sofia", Money.of("15000"));

        List<Balance> balances = this.calculator.calcularBalances(
                Arrays.asList("julian", "diomar", "juan", "sofia"), paid, owed);

        assertEquals(4, balances.size());
        assertEquals(Money.of("45000"), buscar(balances, "julian").getAmount());
        assertEquals(Money.of("-15000"), buscar(balances, "diomar").getAmount());
    }

    @Test
    public void losSaldosSiempreSumanCero() {
        Map<String, Money> paid = new LinkedHashMap<>();
        paid.put("a", Money.of("60000"));
        paid.put("b", Money.of("80000"));
        paid.put("c", Money.of("40000"));

        Map<String, Money> owed = new LinkedHashMap<>();
        owed.put("a", Money.of("51000"));
        owed.put("b", Money.of("47000"));
        owed.put("c", Money.of("43000"));
        owed.put("d", Money.of("39000"));

        List<Balance> balances = this.calculator.calcularBalances(
                Arrays.asList("a", "b", "c", "d"), paid, owed);

        Money sum = Money.ZERO;
        for (Balance balance : balances) {
            sum = sum.plus(balance.getAmount());
        }
        assertTrue(sum.isZero());
    }

    @Test
    public void ordenaDelMayorAcreedorAlMayorDeudor() {
        Map<String, Money> paid = new LinkedHashMap<>();
        paid.put("a", Money.of("10000"));
        paid.put("b", Money.of("50000"));

        Map<String, Money> owed = new LinkedHashMap<>();
        owed.put("a", Money.of("30000"));
        owed.put("b", Money.of("30000"));

        List<Balance> balances = this.calculator.calcularBalances(Arrays.asList("a", "b"), paid, owed);

        assertEquals("b", balances.get(0).getUserId());
        assertEquals("a", balances.get(1).getUserId());
    }

    @Test
    public void elIntegranteSinMovimientosApareceEnCero() {
        List<Balance> balances = this.calculator.calcularBalances(
                Arrays.asList("a", "b"),
                Collections.singletonMap("a", Money.of("10000")),
                Collections.singletonMap("a", Money.of("10000")));

        assertTrue(buscar(balances, "b").isSettled());
    }

    private Balance buscar(List<Balance> balances, String userId) {
        for (Balance balance : balances) {
            if (balance.getUserId().equals(userId)) {
                return balance;
            }
        }
        throw new AssertionError("No se encontró el saldo de " + userId);
    }
}
