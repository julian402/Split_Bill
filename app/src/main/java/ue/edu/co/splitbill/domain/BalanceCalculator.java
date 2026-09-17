package ue.edu.co.splitbill.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calcula el saldo neto de cada participante del grupo.
 *
 * La formula es una sola: saldo = lo que la persona pago - lo que le correspondia pagar.
 * Los dos insumos salen directamente de BalanceDao, que los obtiene con SUM y GROUP BY.
 *
 * Una propiedad util para probar el resultado: la suma de todos los saldos siempre da cero,
 * porque cada peso que alguien puso de mas es un peso que alguien mas quedo debiendo.
 */
public final class BalanceCalculator {

    /**
     * @param userIds      todos los integrantes del grupo, para que aparezcan incluso con saldo en cero
     * @param totalPaid    cuanto pago cada integrante (id del usuario -> monto)
     * @param totalOwed    cuanto le correspondia pagar a cada integrante (id del usuario -> monto)
     */
    public List<Balance> calcularBalances(Collection<String> userIds,
                                          Map<String, Money> totalPaid,
                                          Map<String, Money> totalOwed) {
        if (userIds == null) {
            throw new IllegalArgumentException("Debe indicar los integrantes del grupo");
        }

        //Se toman los integrantes y, por seguridad, cualquier id que aparezca solo en los montos
        Set<String> allIds = new LinkedHashSet<>(userIds);
        if (totalPaid != null) {
            allIds.addAll(totalPaid.keySet());
        }
        if (totalOwed != null) {
            allIds.addAll(totalOwed.keySet());
        }

        List<Balance> balances = new ArrayList<>(allIds.size());
        for (String userId : allIds) {
            Money paid = leerMonto(totalPaid, userId);
            Money owed = leerMonto(totalOwed, userId);
            balances.add(new Balance(userId, paid.minus(owed)));
        }

        //De mayor acreedor a mayor deudor, que es como se muestra en pantalla
        Collections.sort(balances, new Comparator<Balance>() {
            @Override
            public int compare(Balance a, Balance b) {
                return b.getAmount().compareTo(a.getAmount());
            }
        });
        return balances;
    }

    private Money leerMonto(Map<String, Money> amounts, String userId) {
        if (amounts == null) {
            return Money.ZERO;
        }
        Money amount = amounts.get(userId);
        return amount == null ? Money.ZERO : amount;
    }
}
