package ue.edu.co.splitbill.domain.split;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.domain.Share;
import ue.edu.co.splitbill.domain.SplitType;

/**
 * Reparte el gasto segun el monto exacto que el usuario le asigna a cada participante.
 *
 * Sirve cuando cada quien sabe cuanto consumio: en un restaurante, cada plato tiene su precio.
 * Como aqui no hay nada que prorratear, no hereda de WeightedSplitStrategy: implementa la interfaz
 * directamente. Lo unico que hay que verificar es que los montos digitados sumen el total del gasto.
 */
public final class ExactAmountSplitStrategy implements SplitStrategy {

    @Override
    public void validar(SplitRequest request) {
        Money total = request.getTotal();
        if (total.isNegative()) {
            throw new IllegalArgumentException("El monto del gasto no puede ser negativo");
        }
        if (total.isZero()) {
            throw new IllegalArgumentException("El monto del gasto debe ser mayor que cero");
        }

        Money sum = Money.ZERO;
        for (String userId : request.getParticipantIds()) {
            sum = sum.plus(leerMonto(request, userId));
        }
        if (!sum.equals(total)) {
            throw new IllegalArgumentException("Los montos deben sumar " + total.format()
                    + " pero suman " + sum.format());
        }
    }

    @Override
    public List<Share> split(SplitRequest request) {
        validar(request);
        List<Share> shares = new ArrayList<>(request.getParticipantCount());
        for (String userId : request.getParticipantIds()) {
            shares.add(new Share(userId, leerMonto(request, userId)));
        }
        return shares;
    }

    private Money leerMonto(SplitRequest request, String userId) {
        BigDecimal amount = request.getValue(userId);
        if (amount == null) {
            throw new IllegalArgumentException("Falta el monto de uno de los participantes");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Los montos no pueden ser negativos");
        }
        return Money.of(amount);
    }

    @Override
    public SplitType getType() {
        return SplitType.EXACT;
    }
}
