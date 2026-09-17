package ue.edu.co.splitbill.dao;

import ue.edu.co.splitbill.domain.Money;

/**
 * Resultado de una consulta de agregacion: un total en centavos asociado a un usuario.
 *
 * Lo devuelven las dos consultas de BalanceDao (lo pagado y lo adeudado), que son los insumos
 * de BalanceCalculator.
 */
public class UserAmount {

    private String userId;
    private long totalCents;

    public Money getTotal() {
        return Money.ofCents(this.totalCents);
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getTotalCents() {
        return this.totalCents;
    }

    public void setTotalCents(long totalCents) {
        this.totalCents = totalCents;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UserAmount{");
        sb.append("userId=").append(userId);
        sb.append(", total=").append(getTotal().toBigDecimal());
        sb.append('}');
        return sb.toString();
    }
}
