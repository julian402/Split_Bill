package ue.edu.co.splitbill.network.dto;

/**
 * Parte de un gasto: a quien le toca y cuanto, en centavos.
 *
 * Gson llena y lee los campos por su nombre, que es el mismo del JSON del backend.
 */
public class ShareDto {

    private String userId;
    private long amountCents;

    /** Constructor vacio: lo usa Gson para crear el objeto al leer el JSON. */
    public ShareDto() {
    }

    public ShareDto(String userId, long amountCents) {
        this.userId = userId;
        this.amountCents = amountCents;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
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
        final StringBuilder sb = new StringBuilder("ShareDto{");
        sb.append("userId=").append(userId);
        sb.append(", amountCents=").append(amountCents);
        sb.append('}');
        return sb.toString();
    }
}
