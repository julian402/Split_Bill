package ue.edu.co.splitbill.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ue.edu.co.splitbill.entity.SplitType;

/**
 * Gasto con sus partes ya calculadas por la app (la SplitStrategy corre en el celular). El servidor
 * solo comprueba que las partes sumen exactamente el monto.
 *
 * @param id   opcional: UUID generado por la app. Si se repite, el servidor no duplica el gasto
 * @param date opcional: si no llega, se usa la hora del servidor
 */
public record ExpenseRequest(
        UUID id,

        @NotNull(message = "Debe indicar quien pago el gasto")
        UUID payerId,

        @NotBlank(message = "La descripcion del gasto es obligatoria")
        @Size(max = 150, message = "La descripcion es demasiado larga")
        String description,

        @Positive(message = "El monto del gasto debe ser mayor que cero")
        long amountCents,

        @NotNull(message = "Debe indicar como se divide el gasto")
        SplitType splitType,

        Instant date,

        @NotEmpty(message = "El gasto debe tener al menos un participante")
        List<@Valid ShareRequest> shares) {
}
