package ue.edu.co.splitbill.network.dto;

/**
 * Persona tal como la devuelve el servidor. registered indica si tiene cuenta; active, si sigue en el grupo.
 *
 * Gson llena y lee los campos por su nombre, que es el mismo del JSON del backend.
 */
public class UserDto {

    private String id;
    private String names;
    private String email;
    private String phone;
    private boolean registered;
    private boolean active;

    /** Constructor vacio: lo usa Gson para crear el objeto al leer el JSON. */
    public UserDto() {
    }

    public UserDto(String id, String names, String email, String phone, boolean registered, boolean active) {
        this.id = id;
        this.names = names;
        this.email = email;
        this.phone = phone;
        this.registered = registered;
        this.active = active;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
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

    public boolean isRegistered() {
        return this.registered;
    }

    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UserDto{");
        sb.append("id=").append(id);
        sb.append(", names=").append(names);
        sb.append(", email=").append(email);
        sb.append(", phone=").append(phone);
        sb.append(", registered=").append(registered);
        sb.append(", active=").append(active);
        sb.append('}');
        return sb.toString();
    }
}
