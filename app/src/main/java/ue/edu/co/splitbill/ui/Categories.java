package ue.edu.co.splitbill.ui;

import android.content.Context;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.domain.ExpenseCategory;

/**
 * Icono y nombre de cada categoria de gasto. El dominio (ExpenseCategory) no conoce Android: la
 * traduccion a recursos vive aqui, del lado de la interfaz.
 */
public final class Categories {

    private Categories() {
        //Clase de utilidades: no se instancia
    }

    public static int getIcon(ExpenseCategory category) {
        if (category == null) {
            return R.drawable.ic_receipt;
        }
        switch (category) {
            case FOOD:
                return R.drawable.ic_restaurant;
            case GROCERIES:
                return R.drawable.ic_cart;
            case TRANSPORT:
                return R.drawable.ic_car;
            case LODGING:
                return R.drawable.ic_hotel;
            case ENTERTAINMENT:
                return R.drawable.ic_ticket;
            case SERVICES:
                return R.drawable.ic_bolt;
            case PAYMENT:
                return R.drawable.ic_check_circle;
            default:
                return R.drawable.ic_receipt;
        }
    }

    /** Nombre para mostrar, del arreglo expenseCategories (mismo orden que ExpenseCategory.selectable). */
    public static String getName(Context context, ExpenseCategory category) {
        if (category == null || category.isPayment()) {
            return context.getString(R.string.tvCategoryPayment);
        }
        return context.getResources().getStringArray(R.array.expenseCategories)[category.getPosition()];
    }
}
