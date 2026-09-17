package ue.edu.co.splitbill.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ue.edu.co.splitbill.domain.split.PercentageSplitStrategy;
import ue.edu.co.splitbill.domain.split.SplitRequest;
import ue.edu.co.splitbill.domain.split.SplitStrategy;

public class PercentageSplitStrategyTest {

    private SplitStrategy strategy;

    @Before
    public void setUp() {
        this.strategy = new PercentageSplitStrategy();
    }

    @Test
    public void repartePorPorcentajes() {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put("a", new BigDecimal("40"));
        values.put("b", new BigDecimal("30"));
        values.put("c", new BigDecimal("20"));
        values.put("d", new BigDecimal("10"));

        List<Share> shares = this.strategy.split(
                new SplitRequest(Money.of("40000"), Arrays.asList("a", "b", "c", "d"), values));

        assertEquals(Money.of("16000"), shares.get(0).getAmount());
        assertEquals(Money.of("12000"), shares.get(1).getAmount());
        assertEquals(Money.of("8000"), shares.get(2).getAmount());
        assertEquals(Money.of("4000"), shares.get(3).getAmount());
    }

    @Test
    public void elCentavoSobranteVaAlResiduoMasAlto() {
        //10.00 repartido 33.33 / 33.33 / 33.34 deja un centavo sobrante que le toca al tercero
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put("a", new BigDecimal("33.33"));
        values.put("b", new BigDecimal("33.33"));
        values.put("c", new BigDecimal("33.34"));

        List<Share> shares = this.strategy.split(
                new SplitRequest(Money.of("10.00"), Arrays.asList("a", "b", "c"), values));

        assertEquals(Money.of("3.33"), shares.get(0).getAmount());
        assertEquals(Money.of("3.33"), shares.get(1).getAmount());
        assertEquals(Money.of("3.34"), shares.get(2).getAmount());
    }

    @Test
    public void lasPartesSiempreSumanElTotal() {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put("a", new BigDecimal("33.33"));
        values.put("b", new BigDecimal("33.33"));
        values.put("c", new BigDecimal("33.34"));

        for (long cents = 1; cents <= 500; cents++) {
            Money total = Money.ofCents(cents);
            List<Share> shares = this.strategy.split(
                    new SplitRequest(total, Arrays.asList("a", "b", "c"), values));

            Money sum = Money.ZERO;
            for (Share share : shares) {
                sum = sum.plus(share.getAmount());
            }
            assertEquals("Fallo con un total de " + cents + " centavos", total, sum);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaPorcentajesQueNoSuman100() {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put("a", new BigDecimal("50"));
        values.put("b", new BigDecimal("40"));

        this.strategy.split(new SplitRequest(Money.of("10000"), Arrays.asList("a", "b"), values));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaParticipanteSinPorcentaje() {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put("a", new BigDecimal("100"));

        this.strategy.split(new SplitRequest(Money.of("10000"), Arrays.asList("a", "b"), values));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaPorcentajesNegativos() {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put("a", new BigDecimal("110"));
        values.put("b", new BigDecimal("-10"));

        this.strategy.split(new SplitRequest(Money.of("10000"), Arrays.asList("a", "b"), values));
    }
}
