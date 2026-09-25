package ue.edu.co.splitbill.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Cuenta rapida guardada: una cuenta que se dividio al momento, sin grupo ni integrantes registrados
 * (la cena, el taxi). Es de una sola persona, que la guardo para consultarla despues.
 *
 * Igual que en un gasto, el reparto lo calcula la app con sus SplitStrategy; el servidor solo
 * comprueba que las partes sumen exactamente el total (con propina).
 */
@Entity
@Table(name = DatabaseContract.QuickSplits.TABLE_NAME)
public class QuickSplit {

    @Id
    @Column(name = DatabaseContract.QuickSplits.COLUMN_ID)
    private UUID id;

    @Column(name = DatabaseContract.QuickSplits.COLUMN_OWNER_ID, nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = DatabaseContract.QuickSplits.COLUMN_DESCRIPTION, nullable = false)
    private String description;

    @Column(name = DatabaseContract.QuickSplits.COLUMN_SUBTOTAL_CENTS, nullable = false)
    private long subtotalCents;

    @Column(name = DatabaseContract.QuickSplits.COLUMN_TIP_PERCENT, nullable = false)
    private BigDecimal tipPercent;

    @Column(name = DatabaseContract.QuickSplits.COLUMN_TOTAL_CENTS, nullable = false)
    private long totalCents;

    @Enumerated(EnumType.STRING)
    @Column(name = DatabaseContract.QuickSplits.COLUMN_SPLIT_TYPE, nullable = false)
    private SplitType splitType;

    @Column(name = DatabaseContract.QuickSplits.COLUMN_DATE, nullable = false)
    private Instant date;

    @Column(name = DatabaseContract.QuickSplits.COLUMN_STATUS, nullable = false)
    private short status;

    @Column(name = DatabaseContract.QuickSplits.COLUMN_CREATED_AT, nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = DatabaseContract.QuickSplits.COLUMN_UPDATED_AT, nullable = false)
    private Instant updatedAt;

    /** Las partes se guardan junto con la cuenta, en el orden en que se escribieron en la app. */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = DatabaseContract.QuickSplitShares.COLUMN_QUICK_SPLIT_ID, nullable = false)
    @OrderBy("position ASC")
    private List<QuickSplitShare> shares;

    /** Constructor vacio: es el que usa JPA para reconstruir la fila. */
    public QuickSplit() {
        this.id = UUID.randomUUID();
        this.date = Instant.now();
        this.tipPercent = BigDecimal.ZERO;
        this.status = DatabaseContract.STATUS_ACTIVE;
        this.shares = new ArrayList<>();
    }

    public QuickSplit(UUID ownerId, String description, long subtotalCents, BigDecimal tipPercent,
                      long totalCents, SplitType splitType) {
        this();
        this.ownerId = ownerId;
        this.description = description;
        this.subtotalCents = subtotalCents;
        this.tipPercent = tipPercent == null ? BigDecimal.ZERO : tipPercent;
        this.totalCents = totalCents;
        this.splitType = splitType;
    }

    /** Reglas de la cuenta: la propina no resta y las partes suman exactamente el total. */
    public void validar() {
        if (this.description == null || this.description.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la cuenta es obligatorio");
        }
        if (this.subtotalCents <= 0 || this.totalCents <= 0) {
            throw new IllegalArgumentException("El total de la cuenta debe ser mayor que cero");
        }
        if (this.tipPercent.signum() < 0) {
            throw new IllegalArgumentException("La propina no puede ser negativa");
        }
        if (this.totalCents < this.subtotalCents) {
            throw new IllegalArgumentException("El total con propina no puede ser menor que la cuenta");
        }
        if (this.splitType == null) {
            throw new IllegalArgumentException("Debe indicar cómo se divide la cuenta");
        }
        if (this.shares.isEmpty()) {
            throw new IllegalArgumentException("La cuenta debe tener al menos una persona");
        }
        Set<Short> positions = new HashSet<>();
        long total = 0;
        for (QuickSplitShare share : this.shares) {
            if (share.getAmountCents() < 0) {
                throw new IllegalArgumentException("Ninguna parte puede ser negativa");
            }
            if (!positions.add(share.getPosition())) {
                throw new IllegalArgumentException("Dos personas ocupan el mismo lugar en la cuenta");
            }
            total += share.getAmountCents();
        }
        if (total != this.totalCents) {
            throw new IllegalArgumentException("Las partes suman " + total
                    + " centavos pero la cuenta es de " + this.totalCents + " centavos");
        }
    }

    public boolean isActive() {
        return this.status == DatabaseContract.STATUS_ACTIVE;
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

    public UUID getOwnerId() {
        return this.ownerId;
    }

    public String getDescription() {
        return this.description;
    }

    public long getSubtotalCents() {
        return this.subtotalCents;
    }

    public BigDecimal getTipPercent() {
        return this.tipPercent;
    }

    public long getTotalCents() {
        return this.totalCents;
    }

    public SplitType getSplitType() {
        return this.splitType;
    }

    public Instant getDate() {
        return this.date;
    }

    public void setDate(Instant date) {
        this.date = date;
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

    public List<QuickSplitShare> getShares() {
        return this.shares;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("QuickSplit{");
        sb.append("id=").append(id);
        sb.append(", description=").append(description);
        sb.append(", totalCents=").append(totalCents);
        sb.append(", splitType=").append(splitType);
        sb.append(", shares=").append(shares.size());
        sb.append('}');
        return sb.toString();
    }
}
