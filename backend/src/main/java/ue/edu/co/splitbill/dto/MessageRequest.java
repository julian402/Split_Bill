package ue.edu.co.splitbill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje nuevo para el chat del grupo.
 *
 * @param id     opcional: UUID generado por la app. Si se repite, el servidor no lo duplica
 * @param sentAt opcional: hora del celular al escribirlo; si no llega, la del servidor
 */
public record MessageRequest(
        UUID id,

        @NotBlank(message = "El mensaje está vacío")
        @Size(max = 1000, message = "El mensaje no puede tener más de 1000 caracteres")
        String text,

        Instant sentAt) {
}
