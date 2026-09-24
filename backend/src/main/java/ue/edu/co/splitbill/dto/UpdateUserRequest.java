package ue.edu.co.splitbill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos editables del perfil propio. El email no se cambia aqui porque es el usuario de acceso. */
public record UpdateUserRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        String names,

        @Size(max = 30, message = "El telefono es demasiado largo")
        String phone) {
}
