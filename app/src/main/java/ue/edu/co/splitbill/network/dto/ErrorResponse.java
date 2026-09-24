package ue.edu.co.splitbill.network.dto;

/**
 * Cuerpo de error del servidor (ProblemDetail, RFC 9457). detail trae el mensaje para el usuario.
 *
 * Gson llena y lee los campos por su nombre, que es el mismo del JSON del backend.
 */
public class ErrorResponse {

    private int status;
    private String title;
    private String detail;

    /** Constructor vacio: lo usa Gson para crear el objeto al leer el JSON. */
    public ErrorResponse() {
    }

    public ErrorResponse(int status, String title, String detail) {
        this.status = status;
        this.title = title;
        this.detail = detail;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetail() {
        return this.detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ErrorResponse{");
        sb.append("status=").append(status);
        sb.append(", title=").append(title);
        sb.append(", detail=").append(detail);
        sb.append('}');
        return sb.toString();
    }
}
