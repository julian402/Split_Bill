package ue.edu.co.splitbill.entity;

/**
 * Categoria de un gasto. Son las mismas de la app Android.
 *
 * PAYMENT no es un gasto sino un pago entre integrantes ("Marcar como pagado" en la liquidacion):
 * lo paga el deudor y su unica parte es del acreedor, asi los saldos se cancelan sin reglas nuevas.
 */
public enum ExpenseCategory {

    FOOD,
    GROCERIES,
    TRANSPORT,
    LODGING,
    ENTERTAINMENT,
    SERVICES,
    OTHER,
    PAYMENT
}
