package ue.edu.co.splitbill.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear una cuenta. La contrasena llega en texto plano por HTTPS y se convierte en hash
 * BCrypt antes de tocar la base de datos. El maximo de 72 es el limite de BCrypt.
 */
public record RegisterRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        String names,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es valido")
        @Size(max = 150, message = "El email es demasiado largo")
        String email,

        @Size(max = 30, message = "El telefono es demasiado largo")
        String phone,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres")
        String password) {
}
