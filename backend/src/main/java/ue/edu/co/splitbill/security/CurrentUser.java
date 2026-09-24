package ue.edu.co.splitbill.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * Lee el id del usuario que hace la peticion a partir de su token (ya validado por Spring Security).
 *
 * Los controladores reciben el token con @AuthenticationPrincipal Jwt jwt y llaman CurrentUser.id(jwt).
 * El id siempre sale del token, nunca del cuerpo de la peticion: asi nadie puede actuar a nombre de otro.
 */
public final class CurrentUser {

    private CurrentUser() {
        //impide crear objetos de esta clase
    }

    public static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
