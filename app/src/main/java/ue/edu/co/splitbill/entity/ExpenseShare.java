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
import ue.edu.co.splitbill.domain.Share;
import ue.edu.co.splitbill.manager.DatabaseContract;

/**
 * Parte de un gasto que le corresponde a un participante.
 *
 * Es la traduccion a base de datos del objeto Share del dominio: la estrategia de division produce
 * los Share y el repositorio los convierte en filas de esta tabla. La suma de las partes de un gasto
 * es siempre igual al monto del gasto.
 *
 * Al borrar un gasto se borran sus partes en cascada, porque una parte no tiene sentido por si sola.
 */
@Entity(tableName = DatabaseContract.ExpenseShares.TABLE_NAME,
        foreignKeys = {
                @ForeignKey(entity = Expense.class,
                        parentColumns = DatabaseContract.Expenses.COLUMN_ID,
                        childColumns = DatabaseContract.ExpenseShares.COLUMN_EXPENSE_ID,
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = User.class,
                        parentColumns = DatabaseContract.Users.COLUMN_ID,
                        childColumns = DatabaseContract.ExpenseShares.COLUMN_USER_ID)
        },
        indices = {
                @Index(value = DatabaseContract.ExpenseShares.COLUMN_EXPENSE_ID),
                @Index(value = DatabaseContract.ExpenseShares.COLUMN_USER_ID)
        })
public class ExpenseShare {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = DatabaseContract.ExpenseShares.COLUMN_ID)
    private String id;

    @NonNull
    @ColumnInfo(name = DatabaseContract.ExpenseShares.COLUMN_EXPENSE_ID)
    private String expenseId;

    @NonNull
    @ColumnInfo(name = DatabaseContract.ExpenseShares.COLUMN_USER_ID)
    private String userId;

    @ColumnInfo(name = DatabaseContract.ExpenseShares.COLUMN_AMOUNT_CENTS)
    private long amountCents;

    /** Constructor vacio: es el que usa Room para reconstruir la fila. */
    public ExpenseShare() {
        this.id = UUID.randomUUID().toString();
        this.expenseId = "";
        this.userId = "";
    }

    @Ignore
    public ExpenseShare(String expenseId, Share share) {
        this();
        this.expenseId = expenseId;
        this.userId = share.getUserId();
        this.amountCents = share.getAmount().getCents();
    }

    public Money getAmount() {
        return Money.ofCents(this.amountCents);
    }

    public void setAmount(Money amount) {
        this.amountCents = amount == null ? 0L : amount.getCents();
    }

    @NonNull
    public String getId() {
        return this.id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getExpenseId() {
        return this.expenseId;
    }

    public void setExpenseId(@NonNull String expenseId) {
        this.expenseId = expenseId;
    }

    @NonNull
    public String getUserId() {
        return this.userId;
    }

    public void setUserId(@NonNull String userId) {
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
        sb.append("expenseId=").append(expenseId);
        sb.append(", userId=").append(userId);
        sb.append(", amount=").append(getAmount().toBigDecimal());
        sb.append('}');
        return sb.toString();
    }
}
