package ue.edu.co.splitbill.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Gasto de un grupo: cuanto se gasto, en que, quien lo pago y cuanto le toca a cada participante.
 *
 * El monto se guarda en centavos (long) por la misma razon que en la app: en punto flotante
 * 0.1 + 0.2 no da 0.3, y en una aplicacion que reparte dinero eso produce descuadres.
 */
@Entity
@Table(name = DatabaseContract.Expenses.TABLE_NAME)
public class Expense {

    @Id
    @Column(name = DatabaseContract.Expenses.COLUMN_ID)
    private UUID id;

    @Column(name = DatabaseContract.Expenses.COLUMN_GROUP_ID, nullable = false)
    private UUID groupId;

    @Column(name = DatabaseContract.Expenses.COLUMN_PAYER_ID, nullable = false)
    private UUID payerId;

    @Column(name = DatabaseContract.Expenses.COLUMN_DESCRIPTION, nullable = false)
    private String description;

    @Column(name = DatabaseContract.Expenses.COLUMN_AMOUNT_CENTS, nullable = false)
    private long amountCents;

    @Enumerated(EnumType.STRING)
    @Column(name = DatabaseContract.Expenses.COLUMN_SPLIT_TYPE, nullable = false)
    private SplitType splitType;

    @Column(name = DatabaseContract.Expenses.COLUMN_DATE, nullable = false)
    private Instant date;

    @Column(name = DatabaseContract.Expenses.COLUMN_STATUS, nullable = false)
    private short status;

    @Column(name = DatabaseContract.Expenses.COLUMN_CREATED_AT, nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = DatabaseContract.Expenses.COLUMN_UPDATED_AT, nullable = false)
    private Instant updatedAt;

    /**
     * Las partes se guardan y se borran junto con el gasto (cascade + orphanRemoval): nunca existe
     * una parte suelta ni un gasto a medio guardar.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = DatabaseContract.ExpenseShares.COLUMN_EXPENSE_ID, nullable = false)
    private List<ExpenseShare> shares;

    /** Constructor vacio: es el que usa JPA para reconstruir la fila. */
    public Expense() {
        this.id = UUID.randomUUID();
        this.date = Instant.now();
        this.status = DatabaseContract.STATUS_ACTIVE;
        this.shares = new ArrayList<>();
    }

    public Expense(UUID groupId, UUID payerId, String description, long amountCents, SplitType splitType) {
        this();
        this.groupId = groupId;
        this.payerId = payerId;
        this.description = description;
        this.amountCents = amountCents;
        this.splitType = splitType;
    }

    /**
     * Reglas de negocio del gasto. La mas importante es la ultima: las partes deben sumar
     * exactamente el monto, ni un centavo mas ni uno menos.
     */
    public void validar() {
        if (this.description == null || this.description.trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción del gasto es obligatoria");
        }
        if (this.payerId == null) {
            throw new IllegalArgumentException("Debe indicar quién pagó el gasto");
        }
        if (this.amountCents <= 0) {
            throw new IllegalArgumentException("El monto del gasto debe ser mayor que cero");
        }
        if (this.splitType == null) {
            throw new IllegalArgumentException("Debe indicar cómo se divide el gasto");
        }
        if (this.shares.isEmpty()) {
            throw new IllegalArgumentException("El gasto debe tener al menos un participante");
        }
        Set<UUID> participants = new HashSet<>();
        for (ExpenseShare share : this.shares) {
            if (share.getAmountCents() < 0) {
                throw new IllegalArgumentException("Ninguna parte puede ser negativa");
            }
            if (!participants.add(share.getUserId())) {
                throw new IllegalArgumentException("Un participante aparece dos veces en el gasto");
            }
        }
        long total = calcularTotalPartes();
        if (total != this.amountCents) {
            throw new IllegalArgumentException("Las partes suman " + total
                    + " centavos pero el gasto es de " + this.amountCents + " centavos");
        }
    }

    public long calcularTotalPartes() {
        long total = 0;
        for (ExpenseShare share : this.shares) {
            total += share.getAmountCents();
        }
        return total;
    }

    /** Ids de todos los que tienen una parte en el gasto. */
    public Set<UUID> getParticipantIds() {
        Set<UUID> ids = new HashSet<>();
        for (ExpenseShare share : this.shares) {
            ids.add(share.getUserId());
        }
        return ids;
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

    public UUID getGroupId() {
        return this.groupId;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }

    public UUID getPayerId() {
        return this.payerId;
    }

    public void setPayerId(UUID payerId) {
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

    public List<ExpenseShare> getShares() {
        return this.shares;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Expense{");
        sb.append("id=").append(id);
        sb.append(", description=").append(description);
        sb.append(", amountCents=").append(amountCents);
        sb.append(", splitType=").append(splitType);
        sb.append(", shares=").append(shares.size());
        sb.append('}');
        return sb.toString();
    }
}
