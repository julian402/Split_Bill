package ue.edu.co.splitbill.domain.split;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ue.edu.co.splitbill.domain.SplitType;

/**
 * Reparte el gasto segun el porcentaje que el usuario asigna a cada participante.
 *
 * Sirve cuando el consumo fue desigual: en un mercado compartido, uno se lleva el 40 % y los demas
 * el resto. Los porcentajes deben sumar exactamente 100.
 */
public final class PercentageSplitStrategy extends WeightedSplitStrategy {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    @Override
    protected void validarPesos(SplitRequest request) {
        BigDecimal sum = BigDecimal.ZERO;
        for (String userId : request.getParticipantIds()) {
            BigDecimal percentage = request.getValue(userId);
            if (percentage == null) {
                throw new IllegalArgumentException("Falta el porcentaje de uno de los participantes");
            }
            if (percentage.signum() < 0) {
                throw new IllegalArgumentException("Los porcentajes no pueden ser negativos");
            }
            sum = sum.add(percentage);
        }
        if (sum.compareTo(ONE_HUNDRED) != 0) {
            throw new IllegalArgumentException("Los porcentajes deben sumar 100, pero suman " + sum.toPlainString());
        }
    }

    @Override
    protected List<BigDecimal> calcularPesos(SplitRequest request) {
        List<BigDecimal> weights = new ArrayList<>(request.getParticipantCount());
        for (String userId : request.getParticipantIds()) {
            weights.add(request.getValue(userId));
        }
        return weights;
    }

    @Override
    public SplitType getType() {
        return SplitType.PERCENTAGE;
    }
}
