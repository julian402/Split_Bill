package ue.edu.co.splitbill.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Grupo de gastos. Quien lo crea queda como dueno: es el unico que puede editarlo, borrarlo o
 * retirar integrantes.
 */
@Entity
@Table(name = DatabaseContract.Groups.TABLE_NAME)
public class Group {

    @Id
    @Column(name = DatabaseContract.Groups.COLUMN_ID)
    private UUID id;

    @Column(name = DatabaseContract.Groups.COLUMN_NAME, nullable = false)
    private String name;

    @Column(name = DatabaseContract.Groups.COLUMN_CURRENCY, nullable = false, length = 3)
    private String currency;

    @Column(name = DatabaseContract.Groups.COLUMN_OWNER_ID, nullable = false)
    private UUID ownerId;

    @Column(name = DatabaseContract.Groups.COLUMN_STATUS, nullable = false)
    private short status;

    @Column(name = DatabaseContract.Groups.COLUMN_CREATED_AT, nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = DatabaseContract.Groups.COLUMN_UPDATED_AT, nullable = false)
    private Instant updatedAt;

    /** Constructor vacio: es el que usa JPA para reconstruir la fila. */
    public Group() {
        this.id = UUID.randomUUID();
        this.currency = DatabaseContract.DEFAULT_CURRENCY;
        this.status = DatabaseContract.STATUS_ACTIVE;
    }

    public Group(String name, String currency, UUID ownerId) {
        this();
        this.name = name;
        if (currency != null) {
            this.currency = currency;
        }
        this.ownerId = ownerId;
    }

    public void validar() {
        if (this.name == null || this.name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del grupo es obligatorio");
        }
        if (this.currency == null || !this.currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("La moneda debe ser un codigo de tres letras, por ejemplo COP");
        }
        if (this.ownerId == null) {
            throw new IllegalArgumentException("El grupo debe tener un dueno");
        }
    }

    public boolean isActive() {
        return this.status == DatabaseContract.STATUS_ACTIVE;
    }

    public boolean isOwnedBy(UUID userId) {
        return this.ownerId.equals(userId);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return this.id;
    }

    public void setId(UUID id) {
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

    public UUID getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public short getStatus() {
        return this.status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Group{");
        sb.append("id=").append(id);
        sb.append(", name=").append(name);
        sb.append(", currency=").append(currency);
        sb.append(", ownerId=").append(ownerId);
        sb.append('}');
        return sb.toString();
    }
}
