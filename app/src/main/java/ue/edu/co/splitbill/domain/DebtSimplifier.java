package ue.edu.co.splitbill.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Calcula el conjunto minimo de transferencias necesarias para saldar todas las deudas del grupo.
 *
 * Es el problema que plantea el Acta de Constitucion: cuando cada integrante le transfiere por
 * separado a cada uno de los demas, un grupo de n personas termina haciendo hasta n*(n-1)/2
 * transferencias. Para 6 personas son 15 movimientos donde bastan 5.
 *
 * Estrategia (algoritmo voraz):
 *   1. Se separan los saldos en acreedores (les deben) y deudores (deben).
 *   2. Se empareja siempre al mayor acreedor con el mayor deudor y se transfiere el menor de los
 *      dos montos. Con esa transferencia al menos uno de los dos queda en cero y sale del problema.
 *   3. El que queda con remanente vuelve a la cola y se repite.
 *
 * Como cada transferencia elimina por lo menos a una persona del problema, el resultado nunca
 * supera n-1 transferencias. Se usan dos colas de prioridad para que buscar el mayor de cada lado
 * cueste log n en vez de recorrer toda la lista.
 */
public final class DebtSimplifier {

    /**
     * @param balances saldos netos del grupo, tal como los entrega BalanceCalculator
     * @return las transferencias a realizar; lista vacia si el grupo ya esta a paz y salvo
     * @throws IllegalArgumentException si los saldos no suman cero (indica un error de calculo previo)
     */
    public List<Transfer> simplificar(List<Balance> balances) {
        if (balances == null) {
            throw new IllegalArgumentException("Debe indicar los saldos del grupo");
        }
        validarBalances(balances);

        //Mayor acreedor primero
        PriorityQueue<Balance> creditors = new PriorityQueue<>(Math.max(1, balances.size()),
                new Comparator<Balance>() {
                    @Override
                    public int compare(Balance a, Balance b) {
                        return b.getAmount().compareTo(a.getAmount());
                    }
                });
        //Mayor deudor primero (el saldo mas negativo)
        PriorityQueue<Balance> debtors = new PriorityQueue<>(Math.max(1, balances.size()),
                new Comparator<Balance>() {
                    @Override
                    public int compare(Balance a, Balance b) {
                        return a.getAmount().compareTo(b.getAmount());
                    }
                });

        for (Balance balance : balances) {
            if (balance.isCreditor()) {
                creditors.add(balance);
            } else if (balance.isDebtor()) {
                debtors.add(balance);
            }
        }

        List<Transfer> transfers = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Balance creditor = creditors.poll();
            Balance debtor = debtors.poll();

            Money amount = Money.min(creditor.getAmount(), debtor.getAmount().abs());
            transfers.add(new Transfer(debtor.getUserId(), creditor.getUserId(), amount));

            //El que queda con remanente regresa a su cola; el que llego a cero sale del problema
            Money creditorRest = creditor.getAmount().minus(amount);
            if (creditorRest.isPositive()) {
                creditors.add(new Balance(creditor.getUserId(), creditorRest));
            }
            Money debtorRest = debtor.getAmount().plus(amount);
            if (debtorRest.isNegative()) {
                debtors.add(new Balance(debtor.getUserId(), debtorRest));
            }
        }
        return transfers;
    }

    private void validarBalances(List<Balance> balances) {
        Money sum = Money.ZERO;
        for (Balance balance : balances) {
            sum = sum.plus(balance.getAmount());
        }
        if (!sum.isZero()) {
            throw new IllegalArgumentException("Los saldos del grupo deben sumar cero, pero suman "
                    + sum.format());
        }
    }
}
