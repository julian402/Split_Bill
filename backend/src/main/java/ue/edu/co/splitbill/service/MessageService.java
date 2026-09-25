package ue.edu.co.splitbill.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import ue.edu.co.splitbill.dto.MessageRequest;
import ue.edu.co.splitbill.dto.MessageResponse;
import ue.edu.co.splitbill.entity.Message;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.exception.ConflictException;
import ue.edu.co.splitbill.repository.MessageRepository;
import ue.edu.co.splitbill.repository.UserRepository;

/**
 * Chat del grupo. Solo los integrantes lo leen y escriben; quien escribe siempre es quien inicio
 * sesion (no se puede escribir a nombre de otro).
 */
@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository,
                          GroupService groupService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.groupService = groupService;
    }

    /** Sin since: todo el chat. Con since: solo lo que llego al servidor despues. */
    @Transactional(readOnly = true)
    public List<MessageResponse> list(UUID userId, UUID groupId, Instant since) {
        this.groupService.requireMembership(groupId, userId);
        List<Message> messages = since == null
                ? this.messageRepository.findByGroupIdOrderBySentAtAsc(groupId)
                : this.messageRepository.findByGroupIdAndCreatedAtAfterOrderBySentAtAsc(groupId, since);
        Set<UUID> senderIds = new HashSet<>();
        for (Message message : messages) {
            senderIds.add(message.getSenderId());
        }
        Map<UUID, String> names = new HashMap<>();
        for (User sender : this.userRepository.findAllById(senderIds)) {
            names.put(sender.getId(), sender.getNames());
        }
        List<MessageResponse> result = new ArrayList<>();
        for (Message message : messages) {
            result.add(MessageResponse.from(message, names.get(message.getSenderId())));
        }
        return result;
    }

    /** Si la app reenvia un mensaje que ya llego (mismo id), se devuelve el guardado: no se duplica. */
    @Transactional
    public MessageResponse send(UUID userId, UUID groupId, MessageRequest request) {
        this.groupService.requireMembership(groupId, userId);
        String senderNames = this.userRepository.findById(userId).map(User::getNames).orElse(null);
        if (request.id() != null) {
            Optional<Message> existing = this.messageRepository.findById(request.id());
            if (existing.isPresent()) {
                if (existing.get().getGroupId().equals(groupId) && existing.get().getSenderId().equals(userId)) {
                    return MessageResponse.from(existing.get(), senderNames);
                }
                throw new ConflictException("Ya existe un mensaje con ese id");
            }
        }
        Message message = new Message(groupId, userId, request.text().trim());
        if (request.id() != null) {
            message.setId(request.id());
        }
        if (request.sentAt() != null) {
            message.setSentAt(request.sentAt());
        }
        message.validar();
        return MessageResponse.from(this.messageRepository.save(message), senderNames);
    }
}
