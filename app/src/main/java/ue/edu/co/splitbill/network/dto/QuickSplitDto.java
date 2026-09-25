package ue.edu.co.splitbill.network.dto;

import java.math.BigDecimal;
import java.util.List;

/** Cuenta rapida guardada tal como viaja por /api/quick-splits. Montos en centavos. */
public class QuickSplitDto {

    private String id;
    private String description;
    private long subtotalCents;
    private BigDecimal tipPercent;
    private long totalCents;
    private String splitType;
    private String date;
    private List<Share> shares;

    /** Constructor vacio: lo usa Gson para crear el objeto al leer el JSON. */
    public QuickSplitDto() {
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getSubtotalCents() {
        return this.subtotalCents;
    }

    public void setSubtotalCents(long subtotalCents) {
        this.subtotalCents = subtotalCents;
    }

    public BigDecimal getTipPercent() {
        return this.tipPercent;
    }

    public void setTipPercent(BigDecimal tipPercent) {
        this.tipPercent = tipPercent;
    }

    public long getTotalCents() {
        return this.totalCents;
    }

    public void setTotalCents(long totalCents) {
        this.totalCents = totalCents;
    }

    public String getSplitType() {
        return this.splitType;
    }

    public void setSplitType(String splitType) {
        this.splitType = splitType;
    }

    public String getDate() {
        return this.date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<Share> getShares() {
        return this.shares;
    }

    public void setShares(List<Share> shares) {
        this.shares = shares;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("QuickSplitDto{");
        sb.append("id=").append(id);
        sb.append(", description=").append(description);
        sb.append(", totalCents=").append(totalCents);
        sb.append(", shares=").append(shares == null ? 0 : shares.size());
        sb.append('}');
        return sb.toString();
    }

    /** Lo que le toca a una persona de la cuenta, en el orden de la lista. */
    public static class Share {

        private String name;
        private long amountCents;

        /** Constructor vacio: lo usa Gson para crear el objeto al leer el JSON. */
        public Share() {
        }

        public Share(String name, long amountCents) {
            this.name = name;
            this.amountCents = amountCents;
        }

        public String getName() {
            return this.name;
        }

        public long getAmountCents() {
            return this.amountCents;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Share{");
            sb.append("name=").append(name);
            sb.append(", amountCents=").append(amountCents);
            sb.append('}');
            return sb.toString();
        }
    }
}
