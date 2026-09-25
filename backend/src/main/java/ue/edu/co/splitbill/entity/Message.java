package ue.edu.co.splitbill.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje del chat de un grupo. Lo escribe un integrante con cuenta y lo leen todos los integrantes.
 * No se edita ni se borra.
 */
@Entity
@Table(name = DatabaseContract.Messages.TABLE_NAME)
public class Message {

    public static final int MAX_LENGTH = 1000;

    @Id
    @Column(name = DatabaseContract.Messages.COLUMN_ID)
    private UUID id;

    @Column(name = DatabaseContract.Messages.COLUMN_GROUP_ID, nullable = false, updatable = false)
    private UUID groupId;

    @Column(name = DatabaseContract.Messages.COLUMN_SENDER_ID, nullable = false, updatable = false)
    private UUID senderId;

    @Column(name = DatabaseContract.Messages.COLUMN_TEXT, nullable = false, updatable = false)
    private String text;

    /** Hora del celular al escribirlo: el orden en que lo ve la gente. */
    @Column(name = DatabaseContract.Messages.COLUMN_SENT_AT, nullable = false, updatable = false)
    private Instant sentAt;

    /** Hora en que llego al servidor: con ella la app pide "lo nuevo desde". */
    @Column(name = DatabaseContract.Messages.COLUMN_CREATED_AT, nullable = false, updatable = false)
    private Instant createdAt;

    /** Constructor vacio: es el que usa JPA para reconstruir la fila. */
    public Message() {
        this.id = UUID.randomUUID();
        this.sentAt = Instant.now();
    }

    public Message(UUID groupId, UUID senderId, String text) {
        this();
        this.groupId = groupId;
        this.senderId = senderId;
        this.text = text;
    }

    public void validar() {
        if (this.text == null || this.text.trim().isEmpty()) {
            throw new IllegalArgumentException("El mensaje está vacío");
        }
        if (this.text.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("El mensaje no puede tener más de " + MAX_LENGTH + " caracteres");
        }
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return this.id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getGroupId() {
        return this.groupId;
    }

    public UUID getSenderId() {
        return this.senderId;
    }

    public String getText() {
        return this.text;
    }

    public Instant getSentAt() {
        return this.sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Message{");
        sb.append("id=").append(id);
        sb.append(", groupId=").append(groupId);
        sb.append(", senderId=").append(senderId);
        sb.append(", length=").append(text == null ? 0 : text.length());
        sb.append('}');
        return sb.toString();
    }
}
