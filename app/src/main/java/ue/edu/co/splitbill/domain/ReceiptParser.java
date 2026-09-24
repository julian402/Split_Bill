package ue.edu.co.splitbill.domain;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Busca el total en el texto de una factura.
 *
 * El reconocimiento de texto (ML Kit) solo entrega letras: saber cual de todos los numeros es el
 * total es trabajo de esta clase, y por eso vive en el dominio, en Java puro y con pruebas JUnit.
 *
 * Reglas, en orden:
 * 1. Se buscan las lineas que dicen TOTAL (sin contar SUBTOTAL, TOTAL IVA ni TOTAL ARTICULOS). El
 *    valor puede estar en la misma linea o a la derecha, a la misma altura. Si hay varias, gana la
 *    mayor (por ejemplo "TOTAL CON PROPINA" frente a "TOTAL").
 * 2. Si ninguna dice TOTAL, se propone el valor mas grande, sin contar lo que el cliente entrego
 *    (EFECTIVO, CAMBIO...), que suele ser mayor que la cuenta.
 *
 * Los valores se leen en los formatos que aparecen en facturas colombianas: "$ 45.900",
 * "45.900,00", "45,900.00" y "45900".
 */
public final class ReceiptParser {

    /** Un numero con separadores de miles opcionales (punto o coma) y decimales opcionales. */
    private static final Pattern AMOUNT = Pattern.compile(
            "(?<![\\d.,])\\$?\\s?(\\d{1,3}(?:[.,]\\d{3})+(?:[.,]\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)(?![\\d%])");

    /** Lineas que no traen precios: identificacion, fechas, telefonos. */
    private static final String[] NOT_PRICES = {"NIT", "TEL", "CEL", "FECHA", "HORA", "FACTURA", "RESOLUCION",
            "C.C", "CC ", "DIRECCION", "CAJA", "MESA", "AUTORIZ", "PREFIJO", "RANGO"};

    /** Lineas con TOTAL que no son el total de la cuenta. */
    private static final String[] NOT_TOTALS = {"SUBTOTAL", "SUB TOTAL", "TOTAL IVA", "TOTAL IMPUESTO",
            "TOTAL ARTICULOS", "TOTAL ITEMS", "TOTAL UNIDADES", "TOTAL PRODUCTOS", "TOTAL DESCUENTO"};

    /** Lo que el cliente entrego o le devolvieron: no es lo que costo la cuenta. */
    private static final String[] PAYMENT = {"EFECTIVO", "CAMBIO", "RECIBIDO", "VUELTO", "VUELTAS", "PAGO CON"};

    /** Menos de 100 pesos no es un total razonable (suelen ser cantidades o porcentajes). */
    private static final long MIN_CENTS = 100_00L;
    /** Mas de mil millones es un numero de factura o de identificacion. */
    private static final long MAX_CENTS = 1_000_000_000_00L;

    private ReceiptParser() {
        //impide crear objetos de esta clase
    }

    public static ReceiptScan parse(List<ReceiptLine> lines) {
        List<ReceiptLine> sorted = new ArrayList<>(lines);
        Collections.sort(sorted, new Comparator<ReceiptLine>() {
            @Override
            public int compare(ReceiptLine a, ReceiptLine b) {
                return a.getTop() != b.getTop() ? Integer.compare(a.getTop(), b.getTop())
                        : Integer.compare(a.getLeft(), b.getLeft());
            }
        });

        TreeSet<Money> amounts = new TreeSet<>(Collections.<Money>reverseOrder());
        Money largest = null;
        for (ReceiptLine line : sorted) {
            String text = simplify(line.getText());
            if (containsAny(text, NOT_PRICES)) {
                continue;
            }
            for (Money amount : findAmounts(line.getText())) {
                amounts.add(amount);
                if (!containsAny(text, PAYMENT) && !isPaymentRow(line, sorted)
                        && (largest == null || amount.compareTo(largest) > 0)) {
                    largest = amount;
                }
            }
        }

        Money total = findTotal(sorted);
        if (total == null) {
            total = largest;
        }
        if (total != null) {
            amounts.add(total);
        }
        return new ReceiptScan(total, new ArrayList<>(amounts), findMerchant(sorted));
    }

    /** El mayor de los valores que acompanan a una linea que dice TOTAL. */
    private static Money findTotal(List<ReceiptLine> lines) {
        Money best = null;
        for (ReceiptLine line : lines) {
            String text = simplify(line.getText());
            if (!text.contains("TOTAL") || containsAny(text, NOT_TOTALS)) {
                continue;
            }
            List<Money> candidates = findAmounts(line.getText());
            if (candidates.isEmpty()) {
                //el valor esta en otra columna: se busca a la derecha, en la misma fila
                for (ReceiptLine other : lines) {
                    if (other != line && other.getLeft() > line.getLeft() && other.isSameRowAs(line)) {
                        candidates.addAll(findAmounts(other.getText()));
                    }
                }
            }
            for (Money candidate : candidates) {
                if (best == null || candidate.compareTo(best) > 0) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    /** Un valor suelto a la derecha de "EFECTIVO" o "CAMBIO" tampoco es el total. */
    private static boolean isPaymentRow(ReceiptLine line, List<ReceiptLine> lines) {
        for (ReceiptLine other : lines) {
            if (other != line && other.getLeft() < line.getLeft() && other.isSameRowAs(line)
                    && containsAny(simplify(other.getText()), PAYMENT)) {
                return true;
            }
        }
        return false;
    }

    /** La primera linea con palabras (no numeros) suele ser el nombre del comercio. */
    private static String findMerchant(List<ReceiptLine> lines) {
        for (ReceiptLine line : lines) {
            String original = line.getText().trim();
            String text = simplify(original);
            int letters = original.replaceAll("[^\\p{L}]", "").length();
            if (letters >= 3 && letters * 2 >= original.replace(" ", "").length()
                    && !containsAny(text, NOT_PRICES) && !text.contains("TOTAL")) {
                return capitalize(original);
            }
        }
        return null;
    }

    /** Todos los valores de una linea, en centavos, dentro de un rango razonable. */
    static List<Money> findAmounts(String line) {
        List<Money> result = new ArrayList<>();
        Matcher matcher = AMOUNT.matcher(line == null ? "" : line);
        while (matcher.find()) {
            Long cents = toCents(matcher.group(1));
            if (cents != null && cents >= MIN_CENTS && cents <= MAX_CENTS) {
                result.add(Money.ofCents(cents));
            }
        }
        return result;
    }

    /**
     * Convierte el texto de un valor a centavos decidiendo que es separador de miles y que es de
     * decimales:
     * - Con punto y coma, el ultimo de los dos es el decimal ("45.900,50" y "45,900.50").
     * - Con uno solo: si le siguen exactamente tres digitos es de miles ("45.900"); si le siguen uno
     *   o dos, es decimal ("45900,5").
     */
    static Long toCents(String raw) {
        String value = raw.replace(" ", "").replace("$", "");
        int lastDot = value.lastIndexOf('.');
        int lastComma = value.lastIndexOf(',');
        int decimalAt = -1;
        if (lastDot >= 0 && lastComma >= 0) {
            decimalAt = Math.max(lastDot, lastComma);
        } else if (lastDot >= 0 || lastComma >= 0) {
            int separator = Math.max(lastDot, lastComma);
            int digitsAfter = value.length() - separator - 1;
            if (digitsAfter != 3) {
                decimalAt = separator;
            }
        }
        String integerPart = decimalAt >= 0 ? value.substring(0, decimalAt) : value;
        String decimalPart = decimalAt >= 0 ? value.substring(decimalAt + 1) : "";
        integerPart = integerPart.replaceAll("[^0-9]", "");
        if (integerPart.isEmpty() || integerPart.length() > 12) {
            return null;
        }
        BigDecimal amount = new BigDecimal(integerPart + (decimalPart.isEmpty() ? "" : "." + decimalPart));
        return Money.of(amount).getCents();
    }

    private static boolean containsAny(String text, String[] words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /** Mayusculas y sin tildes, para comparar "Total a pagar" con "TOTAL A PAGAR". */
    private static String simplify(String text) {
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toUpperCase(Locale.ROOT);
    }

    /** "RESTAURANTE EL CORRAL" -> "Restaurante El Corral". */
    private static String capitalize(String text) {
        StringBuilder sb = new StringBuilder();
        for (String word : text.toLowerCase(Locale.ROOT).split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }
}
