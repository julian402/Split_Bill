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

import ue.edu.co.splitbill.manager.DatabaseContract;

/**
 * Mensaje del chat de un grupo (version 6). Se guarda primero en el celular, asi que se puede
 * escribir sin conexion: queda PENDING_CREATE y el SyncManager lo envia despues.
 *
 * El nombre de quien lo escribio se guarda con el mensaje: asi se muestra aunque esa persona ya no
 * este en el grupo.
 */
@Entity(tableName = DatabaseContract.Messages.TABLE_NAME,
        foreignKeys = @ForeignKey(entity = Group.class,
                parentColumns = DatabaseContract.Groups.COLUMN_ID,
                childColumns = DatabaseContract.Messages.COLUMN_GROUP_ID,
                onDelete = ForeignKey.CASCADE),
        indices = @Index(value = DatabaseContract.Messages.COLUMN_GROUP_ID))
public class Message {

    public static final int MAX_LENGTH = 1000;

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = DatabaseContract.Messages.COLUMN_ID)
    private String id;

    @NonNull
    @ColumnInfo(name = DatabaseContract.Messages.COLUMN_GROUP_ID)
    private String groupId;

    @ColumnInfo(name = DatabaseContract.Messages.COLUMN_SENDER_ID)
    private String senderId;

    @ColumnInfo(name = DatabaseContract.Messages.COLUMN_SENDER_NAMES)
    private String senderNames;

    @ColumnInfo(name = DatabaseContract.Messages.COLUMN_TEXT)
    private String text;

    /** Hora del celular al escribirlo: el orden del chat. */
    @ColumnInfo(name = DatabaseContract.Messages.COLUMN_SENT_AT)
    private Date sentAt;

    @ColumnInfo(name = DatabaseContract.Messages.COLUMN_SYNC_STATUS)
    private SyncStatus syncStatus;

    /** Constructor vacio: es el que usa Room para reconstruir la fila. */
    public Message() {
        this.id = UUID.randomUUID().toString();
        this.groupId = "";
        this.sentAt = new Date();
        this.syncStatus = SyncStatus.PENDING_CREATE;
    }

    @Ignore
    public Message(String groupId, String senderId, String senderNames, String text) {
        this();
        this.groupId = groupId;
        this.senderId = senderId;
        this.senderNames = senderNames;
        this.text = text == null ? null : text.trim();
    }

    public void validar() {
        if (this.text == null || this.text.isEmpty()) {
            throw new IllegalArgumentException("Escribe un mensaje");
        }
        if (this.text.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("El mensaje no puede tener más de " + MAX_LENGTH + " caracteres");
        }
    }

    /** Todavia no llega al servidor. */
    public boolean isPending() {
        return this.syncStatus != SyncStatus.SYNCED;
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

    public String getSenderId() {
        return this.senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderNames() {
        return this.senderNames;
    }

    public void setSenderNames(String senderNames) {
        this.senderNames = senderNames;
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getSentAt() {
        return this.sentAt;
    }

    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }

    public SyncStatus getSyncStatus() {
        return this.syncStatus;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Message{");
        sb.append("id=").append(id);
        sb.append(", groupId=").append(groupId);
        sb.append(", senderNames=").append(senderNames);
        sb.append(", syncStatus=").append(syncStatus);
        sb.append('}');
        return sb.toString();
    }
}
