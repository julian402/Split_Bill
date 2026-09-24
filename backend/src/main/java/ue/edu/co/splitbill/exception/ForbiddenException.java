package ue.edu.co.splitbill.exception;

/**
 * 403: el usuario ve el recurso pero no puede hacer esa accion (por ejemplo, editar un
 * grupo del que no es dueno).
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
