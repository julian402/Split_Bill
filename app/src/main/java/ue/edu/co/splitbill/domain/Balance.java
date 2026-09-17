package ue.edu.co.splitbill.domain;

import java.util.Objects;

/**
 * Saldo neto de un participante dentro de un grupo.
 *
 * amount positivo  -> al participante le deben (puso mas de lo que le correspondia)
 * amount negativo  -> el participante debe
 * amount en cero   -> esta a paz y salvo
 */
public final class Balance {

    private final String userId;
    private final Money amount;

    public Balance(String userId, Money amount) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("El saldo debe pertenecer a un participante");
        }
        if (amount == null) {
            throw new IllegalArgumentException("El saldo debe tener un monto");
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

    public boolean isCreditor() {
        return this.amount.isPositive();
    }

    public boolean isDebtor() {
        return this.amount.isNegative();
    }

    public boolean isSettled() {
        return this.amount.isZero();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Balance)) {
            return false;
        }
        Balance other = (Balance) o;
        return this.userId.equals(other.userId) && this.amount.equals(other.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.userId, this.amount);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Balance{");
        sb.append("userId=").append(userId);
        sb.append(", amount=").append(amount.toBigDecimal());
        sb.append('}');
        return sb.toString();
    }
}
