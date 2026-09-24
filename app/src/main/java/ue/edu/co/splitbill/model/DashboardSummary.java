package ue.edu.co.splitbill.model;

import java.util.Collections;
import java.util.List;

import ue.edu.co.splitbill.dao.ActivityItem;
import ue.edu.co.splitbill.dao.GroupListItem;
import ue.edu.co.splitbill.domain.Money;

/**
 * Todo lo que muestra la pantalla de inicio, calculado sobre TODOS los grupos de la persona:
 * cuanto se ha gastado, cuanto este mes, cuanto le toco a ella, su saldo, sus grupos y la actividad
 * reciente. Los pagos entre integrantes no cuentan como gasto, pero si en el saldo.
 */
public class DashboardSummary {

    private final Money total;
    private final Money monthTotal;
    private final Money myShare;
    private final Money myBalance;
    private final int expenseCount;
    private final List<GroupListItem> groups;
    private final List<ActivityItem> recent;

    public DashboardSummary(Money total, Money monthTotal, Money myShare, Money myBalance, int expenseCount,
                            List<GroupListItem> groups, List<ActivityItem> recent) {
        this.total = total;
        this.monthTotal = monthTotal;
        this.myShare = myShare;
        this.myBalance = myBalance;
        this.expenseCount = expenseCount;
        this.groups = Collections.unmodifiableList(groups);
        this.recent = Collections.unmodifiableList(recent);
    }

    public Money getTotal() {
        return this.total;
    }

    public Money getMonthTotal() {
        return this.monthTotal;
    }

    public Money getMyShare() {
        return this.myShare;
    }

    /** Positivo: le deben; negativo: debe. Es la suma de sus saldos en cada grupo. */
    public Money getMyBalance() {
        return this.myBalance;
    }

    public int getExpenseCount() {
        return this.expenseCount;
    }

    public int getGroupCount() {
        return this.groups.size();
    }

    public List<GroupListItem> getGroups() {
        return this.groups;
    }

    public List<ActivityItem> getRecent() {
        return this.recent;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DashboardSummary{");
        sb.append("total=").append(total.toBigDecimal());
        sb.append(", monthTotal=").append(monthTotal.toBigDecimal());
        sb.append(", myShare=").append(myShare.toBigDecimal());
        sb.append(", myBalance=").append(myBalance.toBigDecimal());
        sb.append(", groups=").append(groups.size());
        sb.append('}');
        return sb.toString();
    }
}
