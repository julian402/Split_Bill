package ue.edu.co.splitbill.entity;

/**
 * Formas en que se puede repartir un gasto. Son las mismas tres de la app Android.
 *
 * El servidor no reparte: la app calcula las partes con su SplitStrategy y el servidor solo guarda
 * el tipo para mostrarlo y comprueba que las partes sumen el total.
 */
public enum SplitType {

    /** Partes iguales para todos los participantes. */
    EQUAL,

    /** Cada participante tiene un monto exacto. */
    EXACT,

    /** Cada participante tiene un porcentaje del total. */
    PERCENTAGE
}
