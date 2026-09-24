package ue.edu.co.splitbill.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

/** Parte de un gasto: a quien le toca y cuanto, en centavos. */
public record ShareRequest(
        @NotNull(message = "Cada parte debe indicar el participante")
        UUID userId,

        @PositiveOrZero(message = "Ninguna parte puede ser negativa")
        long amountCents) {
}
