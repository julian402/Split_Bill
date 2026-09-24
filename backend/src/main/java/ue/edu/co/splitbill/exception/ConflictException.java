package ue.edu.co.splitbill.exception;

/**
 * 409: la operacion choca con datos existentes (por ejemplo, un email ya registrado).
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
