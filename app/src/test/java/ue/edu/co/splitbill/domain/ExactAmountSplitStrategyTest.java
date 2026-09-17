package ue.edu.co.splitbill.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ue.edu.co.splitbill.domain.split.ExactAmountSplitStrategy;
import ue.edu.co.splitbill.domain.split.SplitRequest;
import ue.edu.co.splitbill.domain.split.SplitStrategy;

public class ExactAmountSplitStrategyTest {

    private SplitStrategy strategy;

    @Before
    public void setUp() {
        this.strategy = new ExactAmountSplitStrategy();
    }

    @Test
    public void respetaLosMontosDigitados() {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put("a", new BigDecimal("25000"));
        values.put("b", new BigDecimal("15000"));
        values.put("c", new BigDecimal("10000"));

        List<Share> shares = this.strategy.split(
                new SplitRequest(Money.of("50000"), Arrays.asList("a", "b", "c"), values));

        assertEquals(Money.of("25000"), shares.get(0).getAmount());
        assertEquals(Money.of("15000"), shares.get(1).getAmount());
        assertEquals(Money.of("10000"), shares.get(2).getAmount());
    }

    @Test
    public void aceptaQueAlguienAporteCero() {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put("a", new BigDecimal("50000"));
        values.put("b", BigDecimal.ZERO);

        List<Share> shares = this.strategy.split(
                new SplitRequest(Money.of("50000"), Arrays.asList("a", "b"), values));

        assertEquals(Money.of("50000"), shares.get(0).getAmount());
        assertEquals(Money.ZERO, shares.get(1).getAmount());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaMontosQueNoSumanElTotal() {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put("a", new BigDecimal("20000"));
        values.put("b", new BigDecimal("20000"));

        this.strategy.split(new SplitRequest(Money.of("50000"), Arrays.asList("a", "b"), values));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaParticipanteSinMonto() {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put("a", new BigDecimal("50000"));

        this.strategy.split(new SplitRequest(Money.of("50000"), Arrays.asList("a", "b"), values));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaMontosNegativos() {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put("a", new BigDecimal("60000"));
        values.put("b", new BigDecimal("-10000"));

        this.strategy.split(new SplitRequest(Money.of("50000"), Arrays.asList("a", "b"), values));
    }
}
