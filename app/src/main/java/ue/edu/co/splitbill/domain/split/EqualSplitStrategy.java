package ue.edu.co.splitbill.domain.split;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ue.edu.co.splitbill.domain.SplitType;

/**
 * Reparte el gasto en partes iguales entre todos los participantes.
 *
 * Es el caso mas comun: la cuenta del almuerzo dividida entre los que comieron.
 * Todos pesan lo mismo, y los centavos que sobran los reparte la clase padre.
 */
public final class EqualSplitStrategy extends WeightedSplitStrategy {

    @Override
    protected void validarPesos(SplitRequest request) {
        //En partes iguales no hay nada que digitar: basta con que haya participantes,
        //y eso ya lo garantiza el constructor de SplitRequest.
    }

    @Override
    protected List<BigDecimal> calcularPesos(SplitRequest request) {
        List<BigDecimal> weights = new ArrayList<>(request.getParticipantCount());
        for (int i = 0; i < request.getParticipantCount(); i++) {
            weights.add(BigDecimal.ONE);
        }
        return weights;
    }

    @Override
    public SplitType getType() {
        return SplitType.EQUAL;
    }
}
