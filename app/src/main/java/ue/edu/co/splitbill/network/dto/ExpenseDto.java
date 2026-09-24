package ue.edu.co.splitbill.network.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Gasto con sus partes. La fecha viaja como texto ISO-8601 (2026-09-24T13:55:21Z).
 *
 * Gson llena y lee los campos por su nombre, que es el mismo del JSON del backend.
 */
public class ExpenseDto {

    private String id;
    private String groupId;
    private String payerId;
    private String description;
    private long amountCents;
    private String splitType;
    /** Version 4. Un servidor anterior no la manda: se toma como OTHER. */
    private String category;
    private String date;
    private List<ShareDto> shares;
    /** false si el gasto fue borrado. Solo llega asi en la sincronizacion incremental. */
    private boolean active;

    /** Constructor vacio: lo usa Gson para crear el objeto al leer el JSON. */
    public ExpenseDto() {
        this.shares = new ArrayList<>();
        //si el servidor no manda el campo, el gasto se toma como activo
        this.active = true;
    }

    public ExpenseDto(String id, String groupId, String payerId, String description, long amountCents, String splitType, String date, List<ShareDto> shares) {
        this.id = id;
        this.groupId = groupId;
        this.payerId = payerId;
        this.description = description;
        this.amountCents = amountCents;
        this.splitType = splitType;
        this.date = date;
        this.shares = shares;
        this.active = true;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getPayerId() {
        return this.payerId;
    }

    public void setPayerId(String payerId) {
        this.payerId = payerId;
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

    public String getSplitType() {
        return this.splitType;
    }

    public void setSplitType(String splitType) {
        this.splitType = splitType;
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDate() {
        return this.date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<ShareDto> getShares() {
        return this.shares;
    }

    public void setShares(List<ShareDto> shares) {
        this.shares = shares;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ExpenseDto{");
        sb.append("id=").append(id);
        sb.append(", groupId=").append(groupId);
        sb.append(", payerId=").append(payerId);
        sb.append(", description=").append(description);
        sb.append(", amountCents=").append(amountCents);
        sb.append(", splitType=").append(splitType);
        sb.append(", category=").append(category);
        sb.append(", date=").append(date);
        sb.append(", shares=").append(shares);
        sb.append('}');
        return sb.toString();
    }
}
