package ue.edu.co.splitbill.dao;

import java.util.Date;

import ue.edu.co.splitbill.domain.Money;

/**
 * Fila de la lista de cuentas rapidas guardadas: lo necesario para mostrarla, sin sus partes.
 * Room la llena con las columnas de DatabaseContract.QuickSplits.SELECT_ACTIVE.
 */
public class QuickSplitListItem {

    private String quickSplitId;
    private String description;
    private long totalCents;
    private Date date;
    private int peopleCount;

    public String getQuickSplitId() {
        return this.quickSplitId;
    }

    public void setQuickSplitId(String quickSplitId) {
        this.quickSplitId = quickSplitId;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getPeopleCount() {
        return this.peopleCount;
    }

    public void setPeopleCount(int peopleCount) {
        this.peopleCount = peopleCount;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("QuickSplitListItem{");
        sb.append("quickSplitId=").append(quickSplitId);
        sb.append(", description=").append(description);
        sb.append(", totalCents=").append(totalCents);
        sb.append(", peopleCount=").append(peopleCount);
        sb.append('}');
        return sb.toString();
    }
}
