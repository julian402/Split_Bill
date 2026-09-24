package ue.edu.co.splitbill.network;

/**
 * El servidor respondio, pero con un error (codigo 4xx o 5xx).
 *
 * Se distingue de IOException, que significa que no hubo respuesta (sin red, servidor apagado). La
 * diferencia importa al sincronizar: sin red se reintenta despues; un rechazo del servidor no se
 * arregla reintentando.
 */
public class ApiException extends RuntimeException {

    private final int code;

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

    public boolean isUnauthorized() {
        return this.code == 401;
    }

    public boolean isNotFound() {
        return this.code == 404;
    }

    /**
     * El servidor rechazo la peticion por su contenido (datos invalidos, sin permiso, conflicto).
     * Repetirla daria el mismo resultado. Se excluyen 401 (sesion vencida), 408 y 429 (el servidor
     * pide esperar), que si pueden funcionar mas tarde.
     */
    public boolean isPermanent() {
        return this.code >= 400 && this.code < 500 && this.code != 401 && this.code != 408 && this.code != 429;
    }
}
