package ue.edu.co.splitbill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Datos para crear o editar un grupo.
 *
 * @param id       opcional: UUID generado por la app. Si no llega, lo genera el servidor
 * @param currency opcional: codigo ISO de tres letras; por defecto COP
 */
public record GroupRequest(
        UUID id,

        @NotBlank(message = "El nombre del grupo es obligatorio")
        @Size(max = 100, message = "El nombre del grupo es demasiado largo")
        String name,

        @Pattern(regexp = "[A-Z]{3}", message = "La moneda debe ser un código de tres letras, por ejemplo COP")
        String currency) {
}
