package ue.edu.co.splitbill.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Propiedades splitbill.jwt.* de application.yml.
 *
 * @param secret     clave para firmar los tokens (HS256): minimo 32 caracteres
 * @param issuer     quien emite el token; se verifica al recibirlo
 * @param expiration cuanto dura un token, por ejemplo 24h
 */
@ConfigurationProperties(prefix = "splitbill.jwt")
public record JwtProperties(String secret, String issuer, Duration expiration) {
}
