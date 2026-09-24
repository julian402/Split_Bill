package ue.edu.co.splitbill.network.dto;

/**
 * Respuesta de registro e inicio de sesion: el token y los datos de quien inicio sesion.
 *
 * Gson llena y lee los campos por su nombre, que es el mismo del JSON del backend.
 */
public class TokenResponse {

    private String token;
    private String tokenType;
    private long expiresIn;
    private UserDto user;

    /** Constructor vacio: lo usa Gson para crear el objeto al leer el JSON. */
    public TokenResponse() {
    }

    public TokenResponse(String token, String tokenType, long expiresIn, UserDto user) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return this.tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return this.expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserDto getUser() {
        return this.user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TokenResponse{");
        sb.append("token=").append(token == null ? null : "****");
        sb.append(", tokenType=").append(tokenType);
        sb.append(", expiresIn=").append(expiresIn);
        sb.append(", user=").append(user);
        sb.append('}');
        return sb.toString();
    }
}
