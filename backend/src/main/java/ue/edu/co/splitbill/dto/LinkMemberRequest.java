package ue.edu.co.splitbill.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Email de la cuenta con la que se vincula a un integrante que se habia agregado solo por nombre. */
public record LinkMemberRequest(
        @NotBlank(message = "Escriba el email de la cuenta")
        @Email(message = "El email no es válido")
        String email) {
}
