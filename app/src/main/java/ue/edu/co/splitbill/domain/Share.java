package ue.edu.co.splitbill.domain;

import java.util.Objects;

/**
 * Parte de un gasto que le corresponde a un participante.
 * Es el resultado de aplicar una SplitStrategy y el insumo del calculo de balances.
 */
public final class Share {

    private final String userId;
    private final Money amount;

    public Share(String userId, Money amount) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("La parte debe pertenecer a un participante");
        }
        if (amount == null) {
            throw new IllegalArgumentException("La parte debe tener un monto");
        }
        this.userId = userId;
        this.amount = amount;
    }

    public String getUserId() {
        return this.userId;
    }

    public Money getAmount() {
        return this.amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Share)) {
            return false;
        }
        Share other = (Share) o;
        return this.userId.equals(other.userId) && this.amount.equals(other.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.userId, this.amount);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Share{");
        sb.append("userId=").append(userId);
        sb.append(", amount=").append(amount.toBigDecimal());
        sb.append('}');
        return sb.toString();
    }
}
