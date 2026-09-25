package ue.edu.co.splitbill.network.dto;

/** Mensaje del chat tal como viaja por /api/groups/{id}/messages. Fechas en ISO-8601. */
public class MessageDto {

    private String id;
    private String groupId;
    private String senderId;
    private String senderNames;
    private String text;
    private String sentAt;
    /** Hora en que llego al servidor. Solo viene en las respuestas. */
    private String createdAt;

    /** Constructor vacio: lo usa Gson para crear el objeto al leer el JSON. */
    public MessageDto() {
    }

    public MessageDto(String id, String text, String sentAt) {
        this.id = id;
        this.text = text;
        this.sentAt = sentAt;
    }

    public String getId() {
        return this.id;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getSenderId() {
        return this.senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderNames() {
        return this.senderNames;
    }

    public void setSenderNames(String senderNames) {
        this.senderNames = senderNames;
    }

    public String getText() {
        return this.text;
    }

    public String getSentAt() {
        return this.sentAt;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MessageDto{");
        sb.append("id=").append(id);
        sb.append(", groupId=").append(groupId);
        sb.append(", senderNames=").append(senderNames);
        sb.append('}');
        return sb.toString();
    }
}
