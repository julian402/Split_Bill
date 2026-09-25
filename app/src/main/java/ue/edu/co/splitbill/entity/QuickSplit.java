package ue.edu.co.splitbill.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.domain.SplitType;
import ue.edu.co.splitbill.manager.DatabaseContract;

/**
 * Cuenta rapida guardada (version 5): una cuenta que se dividio al momento, sin grupo ni integrantes
 * registrados. Se guarda para consultarla despues y se sincroniza con el servidor como los gastos.
 *
 * Los montos van en centavos. La propina se guarda como texto para no perder decimales ("12.5").
 */
@Entity(tableName = DatabaseContract.QuickSplits.TABLE_NAME)
public class QuickSplit {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = DatabaseContract.QuickSplits.COLUMN_ID)
    private String id;

    @ColumnInfo(name = DatabaseContract.QuickSplits.COLUMN_DESCRIPTION)
    private String description;

    /** La cuenta sin propina. */
    @ColumnInfo(name = DatabaseContract.QuickSplits.COLUMN_SUBTOTAL_CENTS)
    private long subtotalCents;

    @ColumnInfo(name = DatabaseContract.QuickSplits.COLUMN_TIP_PERCENT)
    private String tipPercent;

    /** Con propina: es lo que se reparte. */
    @ColumnInfo(name = DatabaseContract.QuickSplits.COLUMN_TOTAL_CENTS)
    private long totalCents;

    @ColumnInfo(name = DatabaseContract.QuickSplits.COLUMN_SPLIT_TYPE)
    private SplitType splitType;

    @ColumnInfo(name = DatabaseContract.QuickSplits.COLUMN_DATE)
    private Date date;

    @ColumnInfo(name = DatabaseContract.QuickSplits.COLUMN_STATUS)
    private int status;

    @ColumnInfo(name = DatabaseContract.QuickSplits.COLUMN_SYNC_STATUS)
    private SyncStatus syncStatus;

    /** Constructor vacio: es el que usa Room para reconstruir la fila. */
    public QuickSplit() {
        this.id = UUID.randomUUID().toString();
        this.tipPercent = "0";
        this.splitType = SplitType.EQUAL;
        this.date = new Date();
        this.status = DatabaseContract.STATUS_ACTIVE;
        this.syncStatus = SyncStatus.PENDING_CREATE;
    }

    @Ignore
    public QuickSplit(String description, Money subtotal, BigDecimal tipPercent, Money total, SplitType splitType) {
        this();
        this.description = description;
        this.subtotalCents = subtotal == null ? 0L : subtotal.getCents();
        this.tipPercent = tipPercent == null ? "0" : tipPercent.stripTrailingZeros().toPlainString();
        this.totalCents = total == null ? 0L : total.getCents();
        this.splitType = splitType;
    }

    /** Reglas de la cuenta antes de guardarla; el servidor comprueba las mismas. */
    public void validar() {
        if (this.description == null || this.description.trim().isEmpty()) {
            throw new IllegalArgumentException("Ponle un nombre a la cuenta");
        }
        if (this.subtotalCents <= 0 || this.totalCents <= 0) {
            throw new IllegalArgumentException("El total de la cuenta debe ser mayor que cero");
        }
        if (this.totalCents < this.subtotalCents) {
            throw new IllegalArgumentException("El total con propina no puede ser menor que la cuenta");
        }
        if (this.splitType == null) {
            throw new IllegalArgumentException("Debe indicar cómo se divide la cuenta");
        }
    }

    public boolean isActive() {
        return this.status == DatabaseContract.STATUS_ACTIVE;
    }

    public Money getSubtotal() {
        return Money.ofCents(this.subtotalCents);
    }

    public Money getTotal() {
        return Money.ofCents(this.totalCents);
    }

    @NonNull
    public String getId() {
        return this.id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getSubtotalCents() {
        return this.subtotalCents;
    }

    public void setSubtotalCents(long subtotalCents) {
        this.subtotalCents = subtotalCents;
    }

    public String getTipPercent() {
        return this.tipPercent;
    }

    public void setTipPercent(String tipPercent) {
        this.tipPercent = tipPercent;
    }

    public long getTotalCents() {
        return this.totalCents;
    }

    public void setTotalCents(long totalCents) {
        this.totalCents = totalCents;
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

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public SyncStatus getSyncStatus() {
        return this.syncStatus;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("QuickSplit{");
        sb.append("id=").append(id);
        sb.append(", description=").append(description);
        sb.append(", total=").append(getTotal().toBigDecimal());
        sb.append(", tipPercent=").append(tipPercent);
        sb.append(", splitType=").append(splitType);
        sb.append('}');
        return sb.toString();
    }
}
