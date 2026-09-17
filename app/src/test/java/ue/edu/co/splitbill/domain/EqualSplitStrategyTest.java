package ue.edu.co.splitbill.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import ue.edu.co.splitbill.domain.split.EqualSplitStrategy;
import ue.edu.co.splitbill.domain.split.SplitRequest;
import ue.edu.co.splitbill.domain.split.SplitStrategy;

public class EqualSplitStrategyTest {

    private SplitStrategy strategy;

    @Before
    public void setUp() {
        this.strategy = new EqualSplitStrategy();
    }

    @Test
    public void divisionExactaEntreCuatro() {
        List<Share> shares = this.strategy.split(
                new SplitRequest(Money.of("60000"), Arrays.asList("a", "b", "c", "d")));

        assertEquals(4, shares.size());
        for (Share share : shares) {
            assertEquals(Money.of("15000"), share.getAmount());
        }
    }

    @Test
    public void repartoConCentavoSobrante() {
        //100.00 entre 3 da 33.333... : el centavo que sobra se le entrega al primer participante
        List<Share> shares = this.strategy.split(
                new SplitRequest(Money.of("100.00"), Arrays.asList("a", "b", "c")));

        assertEquals(Money.of("33.34"), shares.get(0).getAmount());
        assertEquals(Money.of("33.33"), shares.get(1).getAmount());
        assertEquals(Money.of("33.33"), shares.get(2).getAmount());
    }

    @Test
    public void lasPartesSiempreSumanElTotal() {
        //Se prueban muchos totales seguidos para que ningun centavo se pierda por redondeo
        for (long cents = 1; cents <= 500; cents++) {
            Money total = Money.ofCents(cents);
            List<Share> shares = this.strategy.split(
                    new SplitRequest(total, Arrays.asList("a", "b", "c", "d", "e", "f", "g")));

            Money sum = Money.ZERO;
            for (Share share : shares) {
                sum = sum.plus(share.getAmount());
            }
            assertEquals("Fallo con un total de " + cents + " centavos", total, sum);
        }
    }

    @Test
    public void conservaElOrdenDeLosParticipantes() {
        List<Share> shares = this.strategy.split(
                new SplitRequest(Money.of("30000"), Arrays.asList("julian", "diomar", "juan")));

        assertEquals("julian", shares.get(0).getUserId());
        assertEquals("diomar", shares.get(1).getUserId());
        assertEquals("juan", shares.get(2).getUserId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaGastoSinParticipantes() {
        new SplitRequest(Money.of("100"), java.util.Collections.<String>emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaMontoEnCero() {
        this.strategy.split(new SplitRequest(Money.ZERO, Arrays.asList("a", "b")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaMontoNegativo() {
        this.strategy.split(new SplitRequest(Money.of("-100"), Arrays.asList("a", "b")));
    }
}
