package ue.edu.co.splitbill.dao;

import ue.edu.co.splitbill.domain.Money;

/**
 * Una fila de la lista de grupos: el grupo con su total y cuantos integrantes tiene. No es una
 * tabla: es el resultado de DatabaseContract.Groups.SELECT_ACTIVE_WITH_TOTALS.
 */
public class GroupListItem {

    private String groupId;
    private String name;
    private String ownerId;
    private int memberCount;
    private long totalCents;

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
        sb.append('}');
        return sb.toString();
    }
}
