package ue.edu.co.splitbill.domain;

/**
 * Formas en que se puede repartir un gasto entre los participantes.
 *
 * El orden de las constantes coincide con el del arreglo splitTypes de strings.xml,
 * que es el que alimenta el Spinner de AddExpenseActivity.
 */
public enum SplitType {

    /** Partes iguales para todos los participantes. */
    EQUAL,

    /** El usuario digita cuanto le corresponde exactamente a cada participante. */
    EXACT,

    /** El usuario digita que porcentaje del total le corresponde a cada participante. */
    PERCENTAGE;

    public int getPosition() {
        return ordinal();
    }

    public static SplitType fromPosition(int position) {
        SplitType[] values = values();
        if (position < 0 || position >= values.length) {
            throw new IllegalArgumentException("Posición de tipo de división inválida: " + position);
        }
        return values[position];
    }
}
