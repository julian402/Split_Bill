package ue.edu.co.splitbill.dto;

import java.time.Instant;
import java.util.UUID;

import ue.edu.co.splitbill.entity.Message;

/** Mensaje del chat con el nombre de quien lo escribio, para mostrarlo sin otra consulta. */
public record MessageResponse(UUID id, UUID groupId, UUID senderId, String senderNames, String text,
                              Instant sentAt, Instant createdAt) {

    public static MessageResponse from(Message message, String senderNames) {
        return new MessageResponse(message.getId(), message.getGroupId(), message.getSenderId(), senderNames,
                message.getText(), message.getSentAt(), message.getCreatedAt());
    }
}
