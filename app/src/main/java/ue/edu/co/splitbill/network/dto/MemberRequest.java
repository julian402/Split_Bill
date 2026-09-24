package ue.edu.co.splitbill.network.dto;

/**
 * Cuerpo de POST /api/groups/{id}/members. La app manda el UUID y el nombre del integrante.
 *
 * Gson llena y lee los campos por su nombre, que es el mismo del JSON del backend.
 */
public class MemberRequest {

    private String id;
    private String names;
    private String email;

    /** Constructor vacio: lo usa Gson para crear el objeto al leer el JSON. */
    public MemberRequest() {
    }

    public MemberRequest(String id, String names, String email) {
        this.id = id;
        this.names = names;
        this.email = email;
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MemberRequest{");
        sb.append("id=").append(id);
        sb.append(", names=").append(names);
        sb.append(", email=").append(email);
        sb.append('}');
        return sb.toString();
    }
}
