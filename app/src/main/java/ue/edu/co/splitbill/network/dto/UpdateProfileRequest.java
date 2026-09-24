package ue.edu.co.splitbill.network.dto;

/**
 * Cambios del perfil propio (PUT /api/users/me): nombre y telefono. El email no se cambia.
 */
public class UpdateProfileRequest {

    private String names;
    private String phone;

    /** Constructor vacio: lo usa Gson para crear el objeto al leer el JSON. */
    public UpdateProfileRequest() {
    }

    public UpdateProfileRequest(String names, String phone) {
        this.names = names;
        this.phone = phone;
    }

    public String getNames() {
        return this.names;
    }

    public void setNames(String names) {
        this.names = names;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UpdateProfileRequest{");
        sb.append("names=").append(names);
        sb.append(", phone=").append(phone);
        sb.append('}');
        return sb.toString();
    }
}
