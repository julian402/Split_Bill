package ue.edu.co.splitbill.dao;

import java.util.Date;

import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.domain.SplitType;

/**
 * Fila de la lista de gastos: el gasto mas el nombre de quien lo pago.
 *
 * No es una entidad, es el resultado de la consulta con JOIN de DatabaseContract.Expenses. Traer el
 * nombre del pagador en la misma consulta evita hacer una consulta adicional por cada gasto de la
 * lista, que es el error clasico al pintar un RecyclerView.
 */
public class ExpenseListItem {

    private String expenseId;
    private String description;
    private long amountCents;
    private SplitType splitType;
    private Date date;
    private String payerNames;

    public Money getAmount() {
        return Money.ofCents(this.amountCents);
    }

    public String getExpenseId() {
        return this.expenseId;
    }

    public void setExpenseId(String expenseId) {
        this.expenseId = expenseId;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getAmountCents() {
        return this.amountCents;
    }

    public void setAmountCents(long amountCents) {
        this.amountCents = amountCents;
    }

    public SplitType getSplitType() {
        return this.splitType;
    }

    public void setSplitType(SplitType splitType) {
        this.splitType = splitType;
    }

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getPayerNames() {
        return this.payerNames;
    }

    public void setPayerNames(String payerNames) {
        this.payerNames = payerNames;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ExpenseListItem{");
        sb.append("description=").append(description);
        sb.append(", amount=").append(getAmount().toBigDecimal());
        sb.append(", payer=").append(payerNames);
        sb.append('}');
        return sb.toString();
    }
}
