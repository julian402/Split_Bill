package ue.edu.co.splitbill.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;
import java.util.UUID;

import ue.edu.co.splitbill.manager.DatabaseContract;

/**
 * Grupo de gastos.
 *
 * En la entrega 1 la aplicacion trabajaba siempre con el grupo sembrado por defecto; desde la
 * entrega 4 la persona puede tener varios y cambiar entre ellos (GroupsActivity). Quien esta en cada
 * grupo se guarda en group_members.
 */
@Entity(tableName = DatabaseContract.Groups.TABLE_NAME)
public class Group {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = DatabaseContract.Groups.COLUMN_ID)
    private String id;

    @ColumnInfo(name = DatabaseContract.Groups.COLUMN_NAME)
    private String name;

    @ColumnInfo(name = DatabaseContract.Groups.COLUMN_CURRENCY)
    private String currency;

    @ColumnInfo(name = DatabaseContract.Groups.COLUMN_CREATED_AT)
    private Date createdAt;

    @ColumnInfo(name = DatabaseContract.Groups.COLUMN_STATUS)
    private int status;

    /** Agregada en la version 2 del esquema, junto con ownerId (ver SplitBillDatabase.MIGRATION_1_2). */
    @ColumnInfo(name = DatabaseContract.Groups.COLUMN_SYNC_STATUS)
    private SyncStatus syncStatus;

    /** Id en el servidor de quien creo el grupo. Null mientras el grupo no se haya subido. */
    @ColumnInfo(name = DatabaseContract.Groups.COLUMN_OWNER_ID)
    private String ownerId;

    /** Constructor vacio: es el que usa Room para reconstruir la fila. */
    public Group() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = new Date();
        this.currency = DatabaseContract.DEFAULT_GROUP_CURRENCY;
        this.status = DatabaseContract.STATUS_ACTIVE;
        this.syncStatus = SyncStatus.PENDING_CREATE;
    }

    @Ignore
    public Group(String name, String currency) {
        this();
        this.name = name;
        this.currency = currency;
    }

    public void validar() {
        if (this.name == null || this.name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del grupo es obligatorio");
        }
        if (this.name.trim().length() > 100) {
            throw new IllegalArgumentException("El nombre del grupo es demasiado largo");
        }
    }

    public boolean isActive() {
        return this.status == DatabaseContract.STATUS_ACTIVE;
    }

    @NonNull
    public String getId() {
        return this.id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrency() {
        return this.currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Date getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
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

    public String getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Group{");
        sb.append("id=").append(id);
        sb.append(", name=").append(name);
        sb.append(", currency=").append(currency);
        sb.append(", syncStatus=").append(syncStatus);
        sb.append('}');
        return sb.toString();
    }
}
