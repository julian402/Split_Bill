package ue.edu.co.splitbill.network.dto;

/**
 * Cuerpo de POST /api/auth/register.
 *
 * Gson llena y lee los campos por su nombre, que es el mismo del JSON del backend.
 */
public class RegisterRequest {

    private String names;
    private String email;
    private String phone;
    private String password;

    /** Constructor vacio: lo usa Gson para crear el objeto al leer el JSON. */
    public RegisterRequest() {
    }

    public RegisterRequest(String names, String email, String phone, String password) {
        this.names = names;
        this.email = email;
        this.phone = phone;
        this.password = password;
    }

    public String getNames() {
        return this.names;
    }

    public void setNames(String names) {
        this.names = names;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RegisterRequest{");
        sb.append("names=").append(names);
        sb.append(", email=").append(email);
        sb.append(", phone=").append(phone);
        sb.append(", password=****");
        sb.append('}');
        return sb.toString();
    }
}
