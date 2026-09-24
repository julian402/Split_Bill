package ue.edu.co.splitbill.security;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

import ue.edu.co.splitbill.config.JwtProperties;
import ue.edu.co.splitbill.entity.User;

/**
 * Emite los tokens de acceso. El "subject" del token es el id del usuario: con eso cada peticion
 * sabe quien la hace sin consultar la base de datos.
 */
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    public JwtService(JwtEncoder jwtEncoder, JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(this.properties.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(this.properties.expiration()))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("names", user.getNames())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /** Duracion del token en segundos, para informarsela a la app. */
    public long getExpirationSeconds() {
        return this.properties.expiration().toSeconds();
    }
}
