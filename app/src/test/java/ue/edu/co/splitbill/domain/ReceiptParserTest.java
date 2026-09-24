package ue.edu.co.splitbill.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ReceiptParserTest {

    /** Lineas en el orden en que aparecen, una debajo de otra. */
    private static List<ReceiptLine> lines(String... texts) {
        List<ReceiptLine> result = new ArrayList<>();
        for (int i = 0; i < texts.length; i++) {
            result.add(new ReceiptLine(texts[i], i));
        }
        return result;
    }

    @Test
    public void entiendeLosFormatosDeValorColombianos() {
        assertEquals(Long.valueOf(4590000L), ReceiptParser.toCents("45.900"));
        assertEquals(Long.valueOf(4590000L), ReceiptParser.toCents("45,900"));
        assertEquals(Long.valueOf(4590050L), ReceiptParser.toCents("45.900,50"));
        assertEquals(Long.valueOf(4590050L), ReceiptParser.toCents("45,900.50"));
        assertEquals(Long.valueOf(4590000L), ReceiptParser.toCents("45900"));
        assertEquals(Long.valueOf(125000000L), ReceiptParser.toCents("1.250.000"));
        assertEquals(Long.valueOf(1250L), ReceiptParser.toCents("12,50"));
    }

    @Test
    public void tomaElTotalYNoElSubtotal() {
        ReceiptScan scan = ReceiptParser.parse(lines(
                "RESTAURANTE EL CORRAL",
                "NIT 800.123.456-7",
                "2 Hamburguesa        $ 51.800",
                "1 Gaseosa            $ 6.500",
                "SUBTOTAL             $ 58.300",
                "IMPOCONSUMO 8%       $ 4.664",
                "TOTAL                $ 62.964",
                "EFECTIVO             $ 100.000",
                "CAMBIO               $ 37.036"));

        assertEquals(Money.ofCents(6296400L), scan.getTotal());
        assertEquals("Restaurante El Corral", scan.getMerchant());
        //tambien ofrece los demas valores, de mayor a menor
        assertEquals(Money.ofCents(10000000L), scan.getAmounts().get(0));
        assertTrue(scan.getAmounts().contains(Money.ofCents(5830000L)));
    }

    @Test
    public void juntaElTotalConSuValorCuandoEstanEnColumnasSeparadas() {
        List<ReceiptLine> receipt = new ArrayList<>();
        receipt.add(new ReceiptLine("SUBTOTAL", 100, 120, 10));
        receipt.add(new ReceiptLine("40.000", 101, 121, 300));
        receipt.add(new ReceiptLine("TOTAL A PAGAR", 130, 150, 10));
        receipt.add(new ReceiptLine("44.000", 131, 151, 300));
        receipt.add(new ReceiptLine("EFECTIVO", 160, 180, 10));
        receipt.add(new ReceiptLine("50.000", 161, 181, 300));

        assertEquals(Money.ofCents(4400000L), ReceiptParser.parse(receipt).getTotal());
    }

    @Test
    public void conVariosTotalesGanaElMayor() {
        ReceiptScan scan = ReceiptParser.parse(lines(
                "TOTAL 80.000",
                "PROPINA SUGERIDA 10% 8.000",
                "TOTAL CON PROPINA 88.000"));

        assertEquals(Money.ofCents(8800000L), scan.getTotal());
    }

    @Test
    public void sinLaPalabraTotalProponeElValorMasGrandeSinContarElEfectivo() {
        ReceiptScan scan = ReceiptParser.parse(lines(
                "Tienda Don Pepe",
                "Pan 3.500",
                "Leche 4.200",
                "Valor 7.700",
                "Efectivo 20.000"));

        assertEquals(Money.ofCents(770000L), scan.getTotal());
    }

    @Test
    public void ignoraNitTelefonosFechasYPorcentajes() {
        ReceiptScan scan = ReceiptParser.parse(lines(
                "NIT 900.555.111",
                "TEL 3001234567",
                "FECHA 24/09/2026",
                "IVA 19%",
                "TOTAL 15.000"));

        assertEquals(Money.ofCents(1500000L), scan.getTotal());
        assertEquals(1, scan.getAmounts().size());
    }

    @Test
    public void sinValoresDevuelveVacio() {
        ReceiptScan scan = ReceiptParser.parse(lines("Gracias por su compra"));

        assertTrue(scan.isEmpty());
        assertNull(scan.getTotal());
    }
}
