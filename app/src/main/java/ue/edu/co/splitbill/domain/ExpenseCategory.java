package ue.edu.co.splitbill.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Categoria de un gasto: en que se gasto. Solo sirve para mostrarlo (icono y nombre); no cambia
 * como se reparte ni los saldos.
 *
 * PAYMENT es especial: no es un gasto sino un pago entre integrantes ("Marcar como pagado" en la
 * liquidacion). Lo paga el deudor y su unica parte es del acreedor, asi que BalanceCalculator lo
 * suma como cualquier gasto y el saldo de los dos se cancela. No se cuenta en los totales gastados.
 *
 * El orden de las constantes seleccionables coincide con el arreglo expenseCategories de strings.xml.
 */
public enum ExpenseCategory {

    FOOD,
    GROCERIES,
    TRANSPORT,
    LODGING,
    ENTERTAINMENT,
    SERVICES,
    OTHER,
    PAYMENT;

    public boolean isPayment() {
        return this == PAYMENT;
    }

    /** Las que se pueden escoger al registrar un gasto: todas menos PAYMENT. */
    public static List<ExpenseCategory> selectable() {
        List<ExpenseCategory> result = new ArrayList<>();
        for (ExpenseCategory category : values()) {
            if (!category.isPayment()) {
                result.add(category);
            }
        }
        return result;
    }

    public int getPosition() {
        return selectable().indexOf(this);
    }

    public static ExpenseCategory fromPosition(int position) {
        List<ExpenseCategory> values = selectable();
        if (position < 0 || position >= values.size()) {
            throw new IllegalArgumentException("Posición de categoría inválida: " + position);
        }
        return values.get(position);
    }

    /** Un texto desconocido o vacio (por ejemplo, de un servidor viejo) se toma como OTHER. */
    public static ExpenseCategory fromName(String name) {
        if (name == null) {
            return OTHER;
        }
        for (ExpenseCategory category : values()) {
            if (category.name().equals(name)) {
                return category;
            }
        }
        return OTHER;
    }
}
