package ue.edu.co.splitbill.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ue.edu.co.splitbill.entity.SplitType;

/**
 * Cuenta rapida con su reparto ya calculado por la app.
 *
 * @param id         opcional: UUID generado por la app. Si se repite, el servidor no la duplica
 * @param tipPercent opcional: si no llega, sin propina
 * @param date       opcional: si no llega, se usa la hora del servidor
 */
public record QuickSplitRequest(
        UUID id,

        @NotBlank(message = "El nombre de la cuenta es obligatorio")
        @Size(max = 150, message = "El nombre de la cuenta es demasiado largo")
        String description,

        @Positive(message = "El total de la cuenta debe ser mayor que cero")
        long subtotalCents,

        @DecimalMin(value = "0", message = "La propina no puede ser negativa")
        @DecimalMax(value = "999.99", message = "La propina es demasiado alta")
        BigDecimal tipPercent,

        @Positive(message = "El total de la cuenta debe ser mayor que cero")
        long totalCents,

        @NotNull(message = "Debe indicar cómo se divide la cuenta")
        SplitType splitType,

        Instant date,

        @NotEmpty(message = "La cuenta debe tener al menos una persona")
        @Size(max = 50, message = "La cuenta tiene demasiadas personas")
        List<@Valid Share> shares) {

    /** Lo que le toca a una persona: su nombre (como se escribio en la app) y cuanto, en centavos. */
    public record Share(
            @NotBlank(message = "Cada persona debe tener un nombre")
            @Size(max = 80, message = "El nombre es demasiado largo")
            String name,

            @PositiveOrZero(message = "Ninguna parte puede ser negativa")
            long amountCents) {
    }
}
