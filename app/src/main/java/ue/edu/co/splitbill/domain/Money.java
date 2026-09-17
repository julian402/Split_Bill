package ue.edu.co.splitbill.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Objeto de valor inmutable que representa una cantidad de dinero.
 *
 * El monto se guarda internamente como una cantidad entera de centavos y NUNCA como double:
 * en punto flotante 0.1 + 0.2 no da 0.3, y en una aplicacion que reparte dinero entre personas
 * ese error se acumula y produce descuadres visibles para el usuario.
 *
 * La clase es inmutable: no expone setters y toda operacion aritmetica devuelve una instancia nueva.
 */
public final class Money implements Comparable<Money>, Serializable {

    public static final Money ZERO = new Money(0);

    private static final int DECIMALS = 2;
    private static final BigDecimal CENTS_PER_UNIT = BigDecimal.valueOf(100);
    private static final Locale LOCALE_CO = Locale.forLanguageTag("es-CO");

    private final long cents;

    private Money(long cents) {
        this.cents = cents;
    }

    //Fabricas estaticas: son la unica forma de crear un Money

    public static Money ofCents(long cents) {
        return new Money(cents);
    }

    public static Money of(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("El monto no puede ser nulo");
        }
        return new Money(amount.setScale(DECIMALS, RoundingMode.HALF_UP)
                .multiply(CENTS_PER_UNIT)
                .longValueExact());
    }

    public static Money of(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("El monto no puede estar vacio");
        }
        try {
            return of(new BigDecimal(amount.trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El monto " + amount + " no es un numero valido");
        }
    }

    //Operaciones aritmeticas: cada una devuelve un Money nuevo

    public Money plus(Money other) {
        return new Money(this.cents + other.cents);
    }

    public Money minus(Money other) {
        return new Money(this.cents - other.cents);
    }

    public Money times(long factor) {
        return new Money(this.cents * factor);
    }

    public Money negate() {
        return new Money(-this.cents);
    }

    /**
     * Suma un porcentaje al monto. Se usa para agregarle la propina a la cuenta.
     *
     * El porcentaje se aplica sobre los centavos y se redondea al centavo mas cercano, de modo que
     * el total con propina sigue siendo una cantidad exacta que despues se puede repartir sin
     * arrastrar decimales.
     */
    public Money plusPercentage(BigDecimal percentage) {
        if (percentage == null || percentage.signum() == 0) {
            return this;
        }
        if (percentage.signum() < 0) {
            throw new IllegalArgumentException("El porcentaje no puede ser negativo");
        }
        long extra = BigDecimal.valueOf(this.cents)
                .multiply(percentage)
                .divide(CENTS_PER_UNIT, 0, RoundingMode.HALF_UP)
                .longValueExact();
        return new Money(this.cents + extra);
    }

    public Money abs() {
        return this.cents < 0 ? new Money(-this.cents) : this;
    }

    /** Devuelve el menor de los dos montos. Lo usa el algoritmo de liquidacion. */
    public static Money min(Money a, Money b) {
        return a.cents <= b.cents ? a : b;
    }

    //Consultas de estado

    public long getCents() {
        return this.cents;
    }

    public BigDecimal toBigDecimal() {
        return BigDecimal.valueOf(this.cents).divide(CENTS_PER_UNIT, DECIMALS, RoundingMode.UNNECESSARY);
    }

    public boolean isZero() {
        return this.cents == 0;
    }

    public boolean isPositive() {
        return this.cents > 0;
    }

    public boolean isNegative() {
        return this.cents < 0;
    }

    /**
     * Formato de moneda colombiana.
     *
     * Se fuerzan los dos decimales aunque el peso colombiano se maneje normalmente sin centavos:
     * el reparto de un gasto si trabaja con centavos, y ocultarlos haria que dos partes distintas
     * se vieran iguales en pantalla.
     */
    public String format() {
        NumberFormat format = NumberFormat.getCurrencyInstance(LOCALE_CO);
        format.setMinimumFractionDigits(DECIMALS);
        format.setMaximumFractionDigits(DECIMALS);
        return format.format(toBigDecimal());
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(this.cents, other.cents);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money)) {
            return false;
        }
        return this.cents == ((Money) o).cents;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.cents);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Money{");
        sb.append("cents=").append(cents);
        sb.append(", amount=").append(toBigDecimal());
        sb.append('}');
        return sb.toString();
    }
}
