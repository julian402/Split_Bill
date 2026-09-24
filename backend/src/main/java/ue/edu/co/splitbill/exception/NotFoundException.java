package ue.edu.co.splitbill.exception;

/**
 * 404: el recurso no existe o el usuario no tiene acceso a el.
 *
 * Tambien se usa cuando el usuario no pertenece al grupo: responder 404 y no 403 evita revelar
 * que el grupo existe.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
