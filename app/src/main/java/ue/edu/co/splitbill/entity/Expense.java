package ue.edu.co.splitbill.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;
import java.util.UUID;

import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.domain.SplitType;
import ue.edu.co.splitbill.manager.DatabaseContract;

/**
 * Gasto registrado en un grupo: cuanto se gasto, en que y quien lo pago.
 *
 * El monto se guarda como una cantidad entera de centavos y NUNCA como decimal, por la misma razon
 * que explica la clase Money: en punto flotante los montos se descuadran. La conversion entre el
 * entero que vive en la base de datos y el objeto Money del dominio se hace en getAmount().
 */
@Entity(tableName = DatabaseContract.Expenses.TABLE_NAME,
        foreignKeys = {
                @ForeignKey(entity = Group.class,
                        parentColumns = DatabaseContract.Groups.COLUMN_ID,
                        childColumns = DatabaseContract.Expenses.COLUMN_GROUP_ID),
                @ForeignKey(entity = User.class,
                        parentColumns = DatabaseContract.Users.COLUMN_ID,
                        childColumns = DatabaseContract.Expenses.COLUMN_PAYER_ID)
        },
        indices = {
                @Index(value = DatabaseContract.Expenses.COLUMN_GROUP_ID),
                @Index(value = DatabaseContract.Expenses.COLUMN_PAYER_ID)
        })
public class Expense {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = DatabaseContract.Expenses.COLUMN_ID)
    private String id;

    @NonNull
    @ColumnInfo(name = DatabaseContract.Expenses.COLUMN_GROUP_ID)
    private String groupId;

    @NonNull
    @ColumnInfo(name = DatabaseContract.Expenses.COLUMN_PAYER_ID)
    private String payerId;

    @ColumnInfo(name = DatabaseContract.Expenses.COLUMN_DESCRIPTION)
    private String description;

    @ColumnInfo(name = DatabaseContract.Expenses.COLUMN_AMOUNT_CENTS)
    private long amountCents;

    @ColumnInfo(name = DatabaseContract.Expenses.COLUMN_SPLIT_TYPE)
    private SplitType splitType;

    @ColumnInfo(name = DatabaseContract.Expenses.COLUMN_DATE)
    private Date date;

    @ColumnInfo(name = DatabaseContract.Expenses.COLUMN_STATUS)
    private int status;

    @ColumnInfo(name = DatabaseContract.Expenses.COLUMN_SYNC_STATUS)
    private SyncStatus syncStatus;

    /** Constructor vacio: es el que usa Room para reconstruir la fila. */
    public Expense() {
        this.id = UUID.randomUUID().toString();
        this.groupId = DatabaseContract.DEFAULT_GROUP_ID;
        this.payerId = "";
        this.date = new Date();
        this.splitType = SplitType.EQUAL;
        this.status = DatabaseContract.STATUS_ACTIVE;
        this.syncStatus = SyncStatus.PENDING_CREATE;
    }

    @Ignore
    public Expense(String groupId, String payerId, String description, Money amount, SplitType splitType) {
        this();
        this.groupId = groupId;
        this.payerId = payerId;
        this.description = description;
        this.amountCents = amount == null ? 0L : amount.getCents();
        this.splitType = splitType;
    }

    /**
     * Reglas de negocio del gasto. Se llaman antes de guardar, tanto desde la pantalla como desde
     * el repositorio, para que un gasto invalido nunca llegue a la base de datos.
     */
    public void validar() {
        if (this.description == null || this.description.trim().isEmpty()) {
            throw new IllegalArgumentException("La descripcion del gasto es obligatoria");
        }
        if (this.payerId == null || this.payerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar quien pago el gasto");
        }
        if (this.amountCents <= 0) {
            throw new IllegalArgumentException("El monto del gasto debe ser mayor que cero");
        }
        if (this.splitType == null) {
            throw new IllegalArgumentException("Debe indicar como se divide el gasto");
        }
    }

    public boolean isActive() {
        return this.status == DatabaseContract.STATUS_ACTIVE;
    }

    /** Puente entre el entero que guarda SQLite y el objeto de valor del dominio. */
    public Money getAmount() {
        return Money.ofCents(this.amountCents);
    }

    public void setAmount(Money amount) {
        this.amountCents = amount == null ? 0L : amount.getCents();
    }

    @NonNull
    public String getId() {
        return this.id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getGroupId() {
        return this.groupId;
    }

    public void setGroupId(@NonNull String groupId) {
        this.groupId = groupId;
    }

    @NonNull
    public String getPayerId() {
        return this.payerId;
    }

    public void setPayerId(@NonNull String payerId) {
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
        final StringBuilder sb = new StringBuilder("Expense{");
        sb.append("id=").append(id);
        sb.append(", description=").append(description);
        sb.append(", amount=").append(getAmount().toBigDecimal());
        sb.append(", splitType=").append(splitType);
        sb.append('}');
        return sb.toString();
    }
}
