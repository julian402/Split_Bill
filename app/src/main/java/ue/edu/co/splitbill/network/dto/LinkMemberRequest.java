package ue.edu.co.splitbill.network.dto;

/** Email de la cuenta con la que se vincula a un integrante agregado solo por nombre. */
public class LinkMemberRequest {

    private String email;

    /** Constructor vacio: lo usa Gson. */
    public LinkMemberRequest() {
    }

    public LinkMemberRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LinkMemberRequest{");
        sb.append("email=").append(email);
        sb.append('}');
        return sb.toString();
    }
}
