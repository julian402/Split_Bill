package ue.edu.co.splitbill.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;

import ue.edu.co.splitbill.manager.DatabaseContract;

/**
 * Pertenencia de una persona a un grupo (version 3 del esquema, entrega 4).
 *
 * Mientras la app manejaba un solo grupo, "estar en el grupo" era lo mismo que "estar en la tabla
 * users". Con varios grupos una misma persona (por ejemplo quien inicio sesion) esta en varios, y
 * puede salir de uno sin salir de los demas. Por eso el estado activo/retirado y la marca de
 * "pendiente de subir" viven aqui y no en users, igual que en la tabla group_members del backend.
 *
 * La llave es compuesta (grupo, persona): no puede haber dos filas de la misma persona en un grupo.
 */
@Entity(tableName = DatabaseContract.GroupMembers.TABLE_NAME,
        primaryKeys = {DatabaseContract.GroupMembers.COLUMN_GROUP_ID, DatabaseContract.GroupMembers.COLUMN_USER_ID},
        foreignKeys = {
                @ForeignKey(entity = Group.class,
                        parentColumns = DatabaseContract.Groups.COLUMN_ID,
                        childColumns = DatabaseContract.GroupMembers.COLUMN_GROUP_ID,
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = User.class,
                        parentColumns = DatabaseContract.Users.COLUMN_ID,
                        childColumns = DatabaseContract.GroupMembers.COLUMN_USER_ID,
                        onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index(value = DatabaseContract.GroupMembers.COLUMN_USER_ID)})
public class GroupMember {

    @NonNull
    @ColumnInfo(name = DatabaseContract.GroupMembers.COLUMN_GROUP_ID)
    private String groupId;

    @NonNull
    @ColumnInfo(name = DatabaseContract.GroupMembers.COLUMN_USER_ID)
    private String userId;

    @ColumnInfo(name = DatabaseContract.GroupMembers.COLUMN_STATUS)
    private int status;

    @ColumnInfo(name = DatabaseContract.GroupMembers.COLUMN_SYNC_STATUS)
    private SyncStatus syncStatus;

    /** Constructor vacio: es el que usa Room para reconstruir la fila. */
    public GroupMember() {
        this.groupId = "";
        this.userId = "";
        this.status = DatabaseContract.STATUS_ACTIVE;
        this.syncStatus = SyncStatus.PENDING_CREATE;
    }

    @Ignore
    public GroupMember(@NonNull String groupId, @NonNull String userId, SyncStatus syncStatus) {
        this();
        this.groupId = groupId;
        this.userId = userId;
        this.syncStatus = syncStatus;
    }

    public boolean isActive() {
        return this.status == DatabaseContract.STATUS_ACTIVE;
    }

    @NonNull
    public String getGroupId() {
        return this.groupId;
    }

    public void setGroupId(@NonNull String groupId) {
        this.groupId = groupId;
    }

    @NonNull
    public String getUserId() {
        return this.userId;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
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
        final StringBuilder sb = new StringBuilder("GroupMember{");
        sb.append("groupId=").append(groupId);
        sb.append(", userId=").append(userId);
        sb.append(", status=").append(status);
        sb.append(", syncStatus=").append(syncStatus);
        sb.append('}');
        return sb.toString();
    }
}
