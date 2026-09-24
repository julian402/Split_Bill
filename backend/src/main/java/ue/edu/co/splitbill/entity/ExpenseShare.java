package ue.edu.co.splitbill.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Parte de un gasto que le corresponde a un participante, en centavos.
 *
 * No tiene referencia a su gasto: la relacion la maneja Expense, que es el unico que crea y reemplaza
 * sus partes. Asi un gasto y sus partes siempre se guardan juntos.
 */
@Entity
@Table(name = DatabaseContract.ExpenseShares.TABLE_NAME)
public class ExpenseShare {

    @Id
    @Column(name = DatabaseContract.ExpenseShares.COLUMN_ID)
    private UUID id;

    @Column(name = DatabaseContract.ExpenseShares.COLUMN_USER_ID, nullable = false)
    private UUID userId;

    @Column(name = DatabaseContract.ExpenseShares.COLUMN_AMOUNT_CENTS, nullable = false)
    private long amountCents;

    /** Constructor vacio: lo exige JPA. */
    public ExpenseShare() {
        this.id = UUID.randomUUID();
    }

    public ExpenseShare(UUID userId, long amountCents) {
        this();
        this.userId = userId;
        this.amountCents = amountCents;
    }

    public UUID getId() {
        return this.id;
    }

    public UUID getUserId() {
        return this.userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public long getAmountCents() {
        return this.amountCents;
    }

    public void setAmountCents(long amountCents) {
        this.amountCents = amountCents;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ExpenseShare{");
        sb.append("userId=").append(userId);
        sb.append(", amountCents=").append(amountCents);
        sb.append('}');
        return sb.toString();
    }
}
