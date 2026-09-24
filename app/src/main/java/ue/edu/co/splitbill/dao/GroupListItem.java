package ue.edu.co.splitbill.dao;

import androidx.room.Ignore;

import java.util.ArrayList;
import java.util.List;

import ue.edu.co.splitbill.domain.Money;

/**
 * Una fila de la lista de grupos: el grupo con su total, cuantos integrantes y gastos tiene y el
 * saldo de la persona en el. No es una tabla: es el resultado de
 * DatabaseContract.Groups.SELECT_ACTIVE_WITH_TOTALS.
 *
 * memberNames no viene de la consulta (@Ignore): lo llena DashboardRepository con los primeros
 * integrantes, para pintar los avatares pequenos de la tarjeta.
 */
public class GroupListItem {

    private String groupId;
    private String name;
    private String ownerId;
    private int memberCount;
    private int expenseCount;
    private long totalCents;
    private long balanceCents;

    @Ignore
    private List<String> memberNames = new ArrayList<>();

    public String getGroupId() {
        return this.groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public int getMemberCount() {
        return this.memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public int getExpenseCount() {
        return this.expenseCount;
    }

    public void setExpenseCount(int expenseCount) {
        this.expenseCount = expenseCount;
    }

    public long getBalanceCents() {
        return this.balanceCents;
    }

    public void setBalanceCents(long balanceCents) {
        this.balanceCents = balanceCents;
    }

    /** Positivo: al usuario le deben; negativo: el usuario debe; cero: esta al dia. */
    public Money getBalance() {
        return Money.ofCents(this.balanceCents);
    }

    public List<String> getMemberNames() {
        return this.memberNames;
    }

    public void setMemberNames(List<String> memberNames) {
        this.memberNames = memberNames;
    }

    public long getTotalCents() {
        return this.totalCents;
    }

    public void setTotalCents(long totalCents) {
        this.totalCents = totalCents;
    }

    public Money getTotal() {
        return Money.ofCents(this.totalCents);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GroupListItem{");
        sb.append("groupId=").append(groupId);
        sb.append(", name=").append(name);
        sb.append(", memberCount=").append(memberCount);
        sb.append(", totalCents=").append(totalCents);
        sb.append(", balanceCents=").append(balanceCents);
        sb.append('}');
        return sb.toString();
    }
}
