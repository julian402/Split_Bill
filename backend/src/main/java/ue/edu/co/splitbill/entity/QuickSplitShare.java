package ue.edu.co.splitbill.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Lo que le toca a una persona de una cuenta rapida. No es un usuario: solo el nombre que se escribio
 * en la app y su lugar en la lista, en centavos.
 */
@Entity
@Table(name = DatabaseContract.QuickSplitShares.TABLE_NAME)
public class QuickSplitShare {

    @Id
    @Column(name = DatabaseContract.QuickSplitShares.COLUMN_ID)
    private UUID id;

    @Column(name = DatabaseContract.QuickSplitShares.COLUMN_POSITION, nullable = false)
    private short position;

    @Column(name = DatabaseContract.QuickSplitShares.COLUMN_NAME, nullable = false)
    private String name;

    @Column(name = DatabaseContract.QuickSplitShares.COLUMN_AMOUNT_CENTS, nullable = false)
    private long amountCents;

    /** Constructor vacio: lo exige JPA. */
    public QuickSplitShare() {
        this.id = UUID.randomUUID();
    }

    public QuickSplitShare(short position, String name, long amountCents) {
        this();
        this.position = position;
        this.name = name;
        this.amountCents = amountCents;
    }

    public UUID getId() {
        return this.id;
    }

    public short getPosition() {
        return this.position;
    }

    public String getName() {
        return this.name;
    }

    public long getAmountCents() {
        return this.amountCents;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("QuickSplitShare{");
        sb.append("position=").append(position);
        sb.append(", name=").append(name);
        sb.append(", amountCents=").append(amountCents);
        sb.append('}');
        return sb.toString();
    }
}
