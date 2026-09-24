package ue.edu.co.splitbill.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lo que ReceiptParser encontro en una factura: el total propuesto, los demas valores (por si el
 * total no quedo bien, el usuario escoge otro) y el nombre del comercio si lo reconocio.
 */
public class ReceiptScan {

    private final Money total;
    private final List<Money> amounts;
    private final String merchant;

    public ReceiptScan(Money total, List<Money> amounts, String merchant) {
        this.total = total;
        this.amounts = amounts == null ? new ArrayList<Money>() : new ArrayList<>(amounts);
        this.merchant = merchant;
    }

    /** true si no se encontro ningun valor en la foto. */
    public boolean isEmpty() {
        return this.total == null;
    }

    /** Null si no se encontro ningun valor. */
    public Money getTotal() {
        return this.total;
    }

    /** Todos los valores distintos encontrados, de mayor a menor. */
    public List<Money> getAmounts() {
        return Collections.unmodifiableList(this.amounts);
    }

    /** Null si no se reconocio. */
    public String getMerchant() {
        return this.merchant;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ReceiptScan{");
        sb.append("total=").append(total);
        sb.append(", amounts=").append(amounts);
        sb.append(", merchant=").append(merchant);
        sb.append('}');
        return sb.toString();
    }
}
