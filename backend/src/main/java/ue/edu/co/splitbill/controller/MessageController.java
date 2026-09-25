package ue.edu.co.splitbill.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ue.edu.co.splitbill.dto.MessageRequest;
import ue.edu.co.splitbill.dto.MessageResponse;
import ue.edu.co.splitbill.security.CurrentUser;
import ue.edu.co.splitbill.service.MessageService;

@RestController
@RequestMapping("/api/groups/{groupId}/messages")
@Tag(name = "Chat", description = "Mensajes entre los integrantes de un grupo.")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    @Operation(summary = "Leer el chat del grupo",
            description = "En el orden en que se escribieron. Con since (ISO-8601) trae solo lo que llegó después.")
    public List<MessageResponse> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId,
                                      @RequestParam(required = false) Instant since) {
        return this.messageService.list(CurrentUser.id(jwt), groupId, since);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Escribir en el chat", description = "Si el id ya existe, devuelve el mensaje guardado.")
    public MessageResponse send(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID groupId,
                                @Valid @RequestBody MessageRequest request) {
        return this.messageService.send(CurrentUser.id(jwt), groupId, request);
    }
}
