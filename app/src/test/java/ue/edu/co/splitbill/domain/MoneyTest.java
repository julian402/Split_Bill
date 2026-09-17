package ue.edu.co.splitbill.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.math.BigDecimal;

public class MoneyTest {

    @Test
    public void ofStringConvierteAcentavos() {
        assertEquals(6000000L, Money.of("60000").getCents());
        assertEquals(1234L, Money.of("12.34").getCents());
    }

    @Test
    public void sumaYrestaNoPierdenPrecision() {
        //El caso clasico que falla con double: 0.1 + 0.2 no da 0.3
        Money resultado = Money.of("0.10").plus(Money.of("0.20"));
        assertEquals(Money.of("0.30"), resultado);
        assertEquals(30L, resultado.getCents());
    }

    @Test
    public void restaDejaSaldoNegativo() {
        Money resultado = Money.of("10.00").minus(Money.of("15.00"));
        assertTrue(resultado.isNegative());
        assertEquals(-500L, resultado.getCents());
    }

    @Test
    public void absYnegateDevuelvenInstanciasNuevas() {
        Money original = Money.of("-25.50");
        assertEquals(Money.of("25.50"), original.abs());
        assertEquals(Money.of("25.50"), original.negate());
        //El original no cambia: la clase es inmutable
        assertEquals(-2550L, original.getCents());
    }

    @Test
    public void minDevuelveElMenor() {
        assertEquals(Money.of("10.00"), Money.min(Money.of("10.00"), Money.of("20.00")));
        assertEquals(Money.of("10.00"), Money.min(Money.of("20.00"), Money.of("10.00")));
    }

    @Test
    public void estadoDelMonto() {
        assertTrue(Money.ZERO.isZero());
        assertTrue(Money.of("1.00").isPositive());
        assertTrue(Money.of("-1.00").isNegative());
        assertFalse(Money.of("1.00").isNegative());
    }

    @Test
    public void redondeaALosDosDecimales() {
        assertEquals(1235L, Money.of(new BigDecimal("12.345")).getCents());
    }

    @Test
    public void dosMontosIgualesSonElMismoValor() {
        assertEquals(Money.of("99.99"), Money.ofCents(9999L));
        assertEquals(Money.of("99.99").hashCode(), Money.ofCents(9999L).hashCode());
    }

    @Test
    public void laPropinaSeSumaAlTotal() {
        //Propina del 10 % sobre 240.000 da 264.000
        assertEquals(Money.of("264000"), Money.of("240000").plusPercentage(new BigDecimal("10")));
    }

    @Test
    public void propinaEnCeroDejaElTotalIgual() {
        assertEquals(Money.of("240000"), Money.of("240000").plusPercentage(BigDecimal.ZERO));
        assertEquals(Money.of("240000"), Money.of("240000").plusPercentage(null));
    }

    @Test
    public void laPropinaSeRedondeaAlCentavoMasCercano() {
        //10 % de 33.33 son 3.333 centavos, que redondean a 3.33
        assertEquals(Money.of("36.66"), Money.of("33.33").plusPercentage(new BigDecimal("10")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaPropinaNegativa() {
        Money.of("240000").plusPercentage(new BigDecimal("-5"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaTextoQueNoEsNumero() {
        Money.of("abc");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaMontoVacio() {
        Money.of("");
    }
}
