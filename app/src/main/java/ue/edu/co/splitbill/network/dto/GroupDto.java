package ue.edu.co.splitbill.network.dto;

/**
 * Grupo en el servidor. Se usa para enviar (id, name, currency) y para recibir.
 *
 * Gson llena y lee los campos por su nombre, que es el mismo del JSON del backend.
 */
public class GroupDto {

    private String id;
    private String name;
    private String currency;
    private String ownerId;

    /** Constructor vacio: lo usa Gson para crear el objeto al leer el JSON. */
    public GroupDto() {
    }

    public GroupDto(String id, String name, String currency, String ownerId) {
        this.id = id;
        this.name = name;
        this.currency = currency;
        this.ownerId = ownerId;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrency() {
        return this.currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GroupDto{");
        sb.append("id=").append(id);
        sb.append(", name=").append(name);
        sb.append(", currency=").append(currency);
        sb.append(", ownerId=").append(ownerId);
        sb.append('}');
        return sb.toString();
    }
}
