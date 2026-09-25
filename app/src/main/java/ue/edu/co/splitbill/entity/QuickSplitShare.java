package ue.edu.co.splitbill.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.manager.DatabaseContract;

/**
 * Lo que le toca a una persona de una cuenta rapida. No es un integrante: es el nombre que se escribio
 * en la cuenta ("Ana", "Persona 2") y su lugar en la lista. Se borra con su cuenta.
 */
@Entity(tableName = DatabaseContract.QuickSplitShares.TABLE_NAME,
        foreignKeys = @ForeignKey(entity = QuickSplit.class,
                parentColumns = DatabaseContract.QuickSplits.COLUMN_ID,
                childColumns = DatabaseContract.QuickSplitShares.COLUMN_QUICK_SPLIT_ID,
                onDelete = ForeignKey.CASCADE),
        indices = @Index(value = DatabaseContract.QuickSplitShares.COLUMN_QUICK_SPLIT_ID))
public class QuickSplitShare {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = DatabaseContract.QuickSplitShares.COLUMN_ID)
    private String id;

    @NonNull
    @ColumnInfo(name = DatabaseContract.QuickSplitShares.COLUMN_QUICK_SPLIT_ID)
    private String quickSplitId;

    @ColumnInfo(name = DatabaseContract.QuickSplitShares.COLUMN_POSITION)
    private int position;

    @ColumnInfo(name = DatabaseContract.QuickSplitShares.COLUMN_NAME)
    private String name;

    @ColumnInfo(name = DatabaseContract.QuickSplitShares.COLUMN_AMOUNT_CENTS)
    private long amountCents;

    /** Constructor vacio: es el que usa Room para reconstruir la fila. */
    public QuickSplitShare() {
        this.id = UUID.randomUUID().toString();
        this.quickSplitId = "";
    }

    @Ignore
    public QuickSplitShare(String quickSplitId, int position, String name, Money amount) {
        this();
        this.quickSplitId = quickSplitId;
        this.position = position;
        this.name = name;
        this.amountCents = amount == null ? 0L : amount.getCents();
    }

    public Money getAmount() {
        return Money.ofCents(this.amountCents);
    }

    @NonNull
    public String getId() {
        return this.id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getQuickSplitId() {
        return this.quickSplitId;
    }

    public void setQuickSplitId(@NonNull String quickSplitId) {
        this.quickSplitId = quickSplitId;
    }

    public int getPosition() {
        return this.position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getAmountCents() {
        return this.amountCents;
    }

    public void setAmountCents(long amountCents) {
        this.amountCents = amountCents;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("QuickSplitShare{");
        sb.append("position=").append(position);
        sb.append(", name=").append(name);
        sb.append(", amount=").append(getAmount().toBigDecimal());
        sb.append('}');
        return sb.toString();
    }
}
