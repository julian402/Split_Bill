package ue.edu.co.splitbill.dao;

/**
 * Fila de la actividad: un gasto (o pago) de cualquier grupo, con el nombre del grupo. Es el
 * resultado de DatabaseContract.Expenses.SELECT_RECENT_ALL_GROUPS; hereda todo lo demas de la fila
 * de la lista de gastos, que Room tambien llena en las subclases.
 */
public class ActivityItem extends ExpenseListItem {

    private String groupName;

    public String getGroupName() {
        return this.groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ActivityItem{");
        sb.append("description=").append(getDescription());
        sb.append(", group=").append(groupName);
        sb.append(", amount=").append(getAmount().toBigDecimal());
        sb.append('}');
        return sb.toString();
    }
}
