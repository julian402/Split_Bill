package ue.edu.co.splitbill.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ue.edu.co.splitbill.domain.split.SplitRequest;
import ue.edu.co.splitbill.domain.split.SplitStrategyFactory;

/**
 * Prueba de extremo a extremo del dominio: reproduce el caso que se muestra en la sustentacion,
 * pasando por la fabrica de estrategias, el calculo de balances y el algoritmo de liquidacion.
 *
 * Escenario: cuatro personas y tres gastos.
 *   Almuerzo  60.000  pago Julian, en partes iguales
 *   Gasolina  80.000  pago Diomar, en partes iguales
 *   Mercado   40.000  pago Juan,   por porcentajes 40/30/20/10
 */
public class SettlementScenarioTest {

    private static final List<String> GRUPO = Arrays.asList("julian", "diomar", "juan", "sofia");

    @Test
    public void tresGastosSeLiquidanConTresTransferencias() {
        Map<String, Money> pagado = new LinkedHashMap<>();
        Map<String, Money> adeudado = new LinkedHashMap<>();

        registrarGasto(pagado, adeudado, "julian", Money.of("60000"), SplitType.EQUAL, null);
        registrarGasto(pagado, adeudado, "diomar", Money.of("80000"), SplitType.EQUAL, null);
        registrarGasto(pagado, adeudado, "juan", Money.of("40000"), SplitType.PERCENTAGE, porcentajes());

        //El total de lo adeudado tiene que ser igual al total gastado
        assertEquals(Money.of("180000"), sumar(adeudado));
        assertEquals(Money.of("180000"), sumar(pagado));

        List<Balance> balances = new BalanceCalculator().calcularBalances(GRUPO, pagado, adeudado);
        assertEquals(Money.of("9000"), buscar(balances, "julian").getAmount());
        assertEquals(Money.of("33000"), buscar(balances, "diomar").getAmount());
        assertEquals(Money.of("-3000"), buscar(balances, "juan").getAmount());
        assertEquals(Money.of("-39000"), buscar(balances, "sofia").getAmount());

        List<Transfer> transfers = new DebtSimplifier().simplificar(balances);

        //Sin simplificar harian falta hasta 6 transferencias entre cuatro personas
        assertTrue("Se generaron " + transfers.size() + " transferencias", transfers.size() <= 3);
        for (Transfer transfer : transfers) {
            assertTrue(transfer.getAmount().isPositive());
        }
    }

    /** Aplica la estrategia correspondiente y acumula lo pagado y lo adeudado. */
    private void registrarGasto(Map<String, Money> pagado,
                                Map<String, Money> adeudado,
                                String payerId,
                                Money total,
                                SplitType splitType,
                                Map<String, BigDecimal> values) {
        acumular(pagado, payerId, total);

        SplitRequest request = values == null
                ? new SplitRequest(total, GRUPO)
                : new SplitRequest(total, GRUPO, values);

        //La pantalla de registrar gastos hace exactamente esto: pide la estrategia y la aplica
        List<Share> shares = SplitStrategyFactory.create(splitType).split(request);
        for (Share share : shares) {
            acumular(adeudado, share.getUserId(), share.getAmount());
        }
    }

    private Map<String, BigDecimal> porcentajes() {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put("julian", new BigDecimal("40"));
        values.put("diomar", new BigDecimal("30"));
        values.put("juan", new BigDecimal("20"));
        values.put("sofia", new BigDecimal("10"));
        return values;
    }

    private void acumular(Map<String, Money> amounts, String userId, Money amount) {
        Money current = amounts.get(userId);
        amounts.put(userId, current == null ? amount : current.plus(amount));
    }

    private Money sumar(Map<String, Money> amounts) {
        Money sum = Money.ZERO;
        for (Money amount : amounts.values()) {
            sum = sum.plus(amount);
        }
        return sum;
    }

    private Balance buscar(List<Balance> balances, String userId) {
        for (Balance balance : balances) {
            if (balance.getUserId().equals(userId)) {
                return balance;
            }
        }
        throw new AssertionError("No se encontro el saldo de " + userId);
    }
}
