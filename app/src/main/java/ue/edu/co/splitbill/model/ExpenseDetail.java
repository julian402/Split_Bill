package ue.edu.co.splitbill.model;

import java.util.ArrayList;
import java.util.List;

import ue.edu.co.splitbill.dao.ShareListItem;
import ue.edu.co.splitbill.entity.Expense;

/**
 * Todo lo que muestra la pantalla de detalle de un gasto: el gasto, quien lo pago y como se repartio.
 *
 * Lo arma ExpenseRepository en una sola operacion, para que la pantalla reciba los datos completos en
 * un solo callback, igual que SettlementResult para la liquidacion.
 */
public class ExpenseDetail {

    private final Expense expense;
    private final String payerNames;
    private final List<ShareListItem> shares;

    public ExpenseDetail(Expense expense, String payerNames, List<ShareListItem> shares) {
        this.expense = expense;
        this.payerNames = payerNames;
        this.shares = shares == null ? new ArrayList<>() : shares;
    }

    public Expense getExpense() {
        return this.expense;
    }

    public String getPayerNames() {
        return this.payerNames;
    }

    public List<ShareListItem> getShares() {
        return this.shares;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ExpenseDetail{");
        sb.append("expense=").append(expense);
        sb.append(", payer=").append(payerNames);
        sb.append(", shares=").append(shares.size());
        sb.append('}');
        return sb.toString();
    }
}
