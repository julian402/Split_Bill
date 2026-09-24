package ue.edu.co.splitbill.dao;

import ue.edu.co.splitbill.domain.Money;

/**
 * Parte de un gasto con el nombre del participante.
 *
 * No es una entidad: es el resultado de la consulta con JOIN de DatabaseContract.ExpenseShares, igual
 * que ExpenseListItem para la lista de gastos.
 */
public class ShareListItem {

    private String userId;
    private String names;
    private long amountCents;

    public Money getAmount() {
        return Money.ofCents(this.amountCents);
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNames() {
        return this.names;
    }

    public void setNames(String names) {
        this.names = names;
    }

    public long getAmountCents() {
        return this.amountCents;
    }

    public void setAmountCents(long amountCents) {
        this.amountCents = amountCents;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ShareListItem{");
        sb.append("names=").append(names);
        sb.append(", amountCents=").append(amountCents);
        sb.append('}');
        return sb.toString();
    }
}
