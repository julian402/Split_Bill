package ue.edu.co.splitbill.domain.split;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.domain.Share;

/**
 * Base de las estrategias que reparten el total segun un peso por participante.
 *
 * Aqui vive una sola vez el reparto de los centavos que sobran, que es la parte delicada del calculo:
 * si 100.000 se divide entre 3, la division exacta da 33.333,33 y al truncar se pierde un centavo.
 * Se usa el metodo del residuo mayor: se asigna a cada participante la parte entera que le corresponde
 * y los centavos sobrantes se entregan, de a uno, a los participantes con mayor residuo. En caso de
 * empate gana el que aparece primero, de modo que el resultado es siempre el mismo para la misma
 * entrada. Asi la suma de las partes queda EXACTAMENTE igual al total.
 *
 * Las subclases solo deciden cuanto pesa cada participante (metodo plantilla).
 */
abstract class WeightedSplitStrategy implements SplitStrategy {

    private static final int SCALE = 10;

    /** Peso de cada participante, en el mismo orden de request.getParticipantIds(). */
    protected abstract List<BigDecimal> calcularPesos(SplitRequest request);

    /** Validaciones propias de cada subclase, ademas de las comunes. */
    protected abstract void validarPesos(SplitRequest request);

    @Override
    public final void validar(SplitRequest request) {
        Money total = request.getTotal();
        if (total.isNegative()) {
            throw new IllegalArgumentException("El monto del gasto no puede ser negativo");
        }
        if (total.isZero()) {
            throw new IllegalArgumentException("El monto del gasto debe ser mayor que cero");
        }
        validarPesos(request);
    }

    @Override
    public final List<Share> split(SplitRequest request) {
        validar(request);
        return repartir(request.getTotal(), request.getParticipantIds(), calcularPesos(request));
    }

    private static List<Share> repartir(Money total, List<String> participantIds, List<BigDecimal> weights) {
        int count = participantIds.size();
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (BigDecimal weight : weights) {
            totalWeight = totalWeight.add(weight);
        }
        if (totalWeight.signum() <= 0) {
            throw new IllegalArgumentException("La suma de las partes debe ser mayor que cero");
        }

        BigDecimal totalCents = BigDecimal.valueOf(total.getCents());
        long[] assigned = new long[count];
        BigDecimal[] remainders = new BigDecimal[count];
        long assignedSum = 0;

        //Primera pasada: a cada quien su parte entera de centavos, guardando el residuo
        for (int i = 0; i < count; i++) {
            BigDecimal exact = totalCents.multiply(weights.get(i))
                    .divide(totalWeight, SCALE, RoundingMode.HALF_UP);
            BigDecimal floor = exact.setScale(0, RoundingMode.FLOOR);
            assigned[i] = floor.longValueExact();
            remainders[i] = exact.subtract(floor);
            assignedSum += assigned[i];
        }

        //Segunda pasada: los centavos sobrantes van a los residuos mas altos
        long leftover = total.getCents() - assignedSum;
        if (leftover > 0) {
            Integer[] order = new Integer[count];
            for (int i = 0; i < count; i++) {
                order[i] = i;
            }
            Arrays.sort(order, new ResidueComparator(remainders));
            for (int k = 0; k < leftover; k++) {
                assigned[order[k]]++;
            }
        }

        List<Share> shares = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            shares.add(new Share(participantIds.get(i), Money.ofCents(assigned[i])));
        }
        return shares;
    }

    /** Ordena por residuo descendente y, ante un empate, por posicion ascendente. */
    private static final class ResidueComparator implements Comparator<Integer> {

        private final BigDecimal[] remainders;

        private ResidueComparator(BigDecimal[] remainders) {
            this.remainders = remainders;
        }

        @Override
        public int compare(Integer a, Integer b) {
            int byResidue = this.remainders[b].compareTo(this.remainders[a]);
            return byResidue != 0 ? byResidue : Integer.compare(a, b);
        }
    }
}
