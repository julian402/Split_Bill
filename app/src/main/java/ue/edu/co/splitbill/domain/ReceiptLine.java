package ue.edu.co.splitbill.domain;

/**
 * Una linea de texto reconocida en la foto de una factura, con su posicion vertical.
 *
 * La posicion hace falta porque en muchas facturas la palabra "TOTAL" esta a la izquierda y el valor
 * a la derecha, y el reconocimiento de texto los entrega como dos lineas separadas: se juntan
 * porque estan a la misma altura.
 *
 * No depende de ML Kit: la pantalla de escaneo convierte el resultado de ML Kit a esta clase, y asi
 * ReceiptParser se puede probar con JUnit sin celular.
 */
public class ReceiptLine {

    private final String text;
    private final int top;
    private final int bottom;
    private final int left;

    public ReceiptLine(String text, int top, int bottom, int left) {
        this.text = text == null ? "" : text;
        this.top = top;
        this.bottom = bottom;
        this.left = left;
    }

    /** Linea sin posicion: las lineas se ordenan como llegan (util en las pruebas). */
    public ReceiptLine(String text, int index) {
        this(text, index * 10, index * 10 + 8, 0);
    }

    /** true si las dos lineas estan a la misma altura (se cruzan en mas de la mitad de su alto). */
    public boolean isSameRowAs(ReceiptLine other) {
        int overlap = Math.min(this.bottom, other.bottom) - Math.max(this.top, other.top);
        int height = Math.min(this.bottom - this.top, other.bottom - other.top);
        return height > 0 && overlap * 2 >= height;
    }

    public String getText() {
        return this.text;
    }

    public int getTop() {
        return this.top;
    }

    public int getBottom() {
        return this.bottom;
    }

    public int getLeft() {
        return this.left;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ReceiptLine{");
        sb.append("text=").append(text);
        sb.append(", top=").append(top);
        sb.append(", left=").append(left);
        sb.append('}');
        return sb.toString();
    }
}
