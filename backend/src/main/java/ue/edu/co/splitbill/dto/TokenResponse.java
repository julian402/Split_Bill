package ue.edu.co.splitbill.dto;

/**
 * Respuesta de registro e inicio de sesion. La app guarda el token y lo envia en cada peticion con
 * el encabezado "Authorization: Bearer (token)".
 *
 * @param expiresIn segundos que dura el token
 */
public record TokenResponse(String token, String tokenType, long expiresIn, UserResponse user) {

    public TokenResponse(String token, long expiresIn, UserResponse user) {
        this(token, "Bearer", expiresIn, user);
    }
}
