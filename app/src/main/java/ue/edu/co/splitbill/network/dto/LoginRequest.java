package ue.edu.co.splitbill.network.dto;

/**
 * Cuerpo de POST /api/auth/login.
 *
 * Gson llena y lee los campos por su nombre, que es el mismo del JSON del backend.
 */
public class LoginRequest {

    private String email;
    private String password;

    /** Constructor vacio: lo usa Gson para crear el objeto al leer el JSON. */
    public LoginRequest() {
    }

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LoginRequest{");
        sb.append("email=").append(email);
        sb.append(", password=****");
        sb.append('}');
        return sb.toString();
    }
}
