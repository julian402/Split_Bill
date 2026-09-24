package ue.edu.co.splitbill.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Integrante que se agrega a un grupo. Se usa de una de dos formas:
 * - Con email: se agrega a una persona que ya tiene cuenta.
 * - Con nombre: se crea un integrante sin cuenta, como en la app de la entrega 1. En ese caso el id
 *   es opcional y lo puede mandar la app para conservar el UUID que ya tenia en el celular.
 */
public record MemberRequest(
        UUID id,

        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        String names,

        @Email(message = "El email no es válido")
        String email) {
}
