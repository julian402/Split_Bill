package ue.edu.co.splitbill.domain;

import java.util.Objects;

/**
 * Transferencia propuesta por el algoritmo de liquidacion:
 * "fromUserId le paga amount a toUserId".
 */
public final class Transfer {

    private final String fromUserId;
    private final String toUserId;
    private final Money amount;

    public Transfer(String fromUserId, String toUserId, Money amount) {
        if (fromUserId == null || toUserId == null) {
            throw new IllegalArgumentException("La transferencia necesita origen y destino");
        }
        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("Una persona no puede transferirse dinero a sí misma");
        }
        if (amount == null || !amount.isPositive()) {
            throw new IllegalArgumentException("La transferencia debe tener un monto positivo");
        }
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.amount = amount;
    }

    public String getFromUserId() {
        return this.fromUserId;
    }

    public String getToUserId() {
        return this.toUserId;
    }

    public Money getAmount() {
        return this.amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Transfer)) {
            return false;
        }
        Transfer other = (Transfer) o;
        return this.fromUserId.equals(other.fromUserId)
                && this.toUserId.equals(other.toUserId)
                && this.amount.equals(other.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.fromUserId, this.toUserId, this.amount);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Transfer{");
        sb.append("from=").append(fromUserId);
        sb.append(", to=").append(toUserId);
        sb.append(", amount=").append(amount.toBigDecimal());
        sb.append('}');
        return sb.toString();
    }
}
