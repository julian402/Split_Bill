package ue.edu.co.splitbill.exception;

/**
 * 401: email o contrasena incorrectos. El mensaje es el mismo en los dos casos para no
 * revelar que correos estan registrados.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
