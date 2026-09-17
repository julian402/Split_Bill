package ue.edu.co.splitbill.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebtSimplifierTest {

    private DebtSimplifier simplifier;

    @Before
    public void setUp() {
        this.simplifier = new DebtSimplifier();
    }

    @Test
    public void grupoSinDeudasNoGeneraTransferencias() {
        List<Balance> balances = Arrays.asList(
                new Balance("a", Money.ZERO),
                new Balance("b", Money.ZERO));

        assertTrue(this.simplifier.simplificar(balances).isEmpty());
    }

    @Test
    public void unDeudorYunAcreedorGeneranUnaSolaTransferencia() {
        List<Balance> balances = Arrays.asList(
                new Balance("a", Money.of("15000")),
                new Balance("b", Money.of("-15000")));

        List<Transfer> transfers = this.simplifier.simplificar(balances);

        assertEquals(1, transfers.size());
        assertEquals("b", transfers.get(0).getFromUserId());
        assertEquals("a", transfers.get(0).getToUserId());
        assertEquals(Money.of("15000"), transfers.get(0).getAmount());
    }

    @Test
    public void cuatroPersonasSeSaldanConTresTransferenciasOmenos() {
        List<Balance> balances = Arrays.asList(
                new Balance("a", Money.of("60.00")),
                new Balance("b", Money.of("20.00")),
                new Balance("c", Money.of("-30.00")),
                new Balance("d", Money.of("-50.00")));

        List<Transfer> transfers = this.simplifier.simplificar(balances);

        assertTrue("Se esperaban maximo 3 transferencias y se generaron " + transfers.size(),
                transfers.size() <= 3);
        verificarQueTodosQuedanEnCero(balances, transfers);
    }

    @Test
    public void seisPersonasSeSaldanConCincoTransferenciasOmenos() {
        List<Balance> balances = Arrays.asList(
                new Balance("a", Money.of("120.00")),
                new Balance("b", Money.of("45.50")),
                new Balance("c", Money.of("10.25")),
                new Balance("d", Money.of("-15.75")),
                new Balance("e", Money.of("-60.00")),
                new Balance("f", Money.of("-100.00")));

        List<Transfer> transfers = this.simplifier.simplificar(balances);

        //Sin simplificar, seis personas transfiriendo una a una darian hasta 15 movimientos
        assertTrue("Se esperaban maximo 5 transferencias y se generaron " + transfers.size(),
                transfers.size() <= 5);
        verificarQueTodosQuedanEnCero(balances, transfers);
    }

    @Test
    public void todasLasTransferenciasTienenMontoPositivo() {
        List<Balance> balances = Arrays.asList(
                new Balance("a", Money.of("33.34")),
                new Balance("b", Money.of("-16.67")),
                new Balance("c", Money.of("-16.67")));

        for (Transfer transfer : this.simplifier.simplificar(balances)) {
            assertTrue(transfer.getAmount().isPositive());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rechazaSaldosQueNoSumanCero() {
        this.simplifier.simplificar(Collections.singletonList(new Balance("a", Money.of("10000"))));
    }

    /** Aplica las transferencias sobre los saldos originales: todos deben terminar en cero. */
    private void verificarQueTodosQuedanEnCero(List<Balance> balances, List<Transfer> transfers) {
        Map<String, Money> saldos = new HashMap<>();
        for (Balance balance : balances) {
            saldos.put(balance.getUserId(), balance.getAmount());
        }
        for (Transfer transfer : transfers) {
            saldos.put(transfer.getFromUserId(),
                    saldos.get(transfer.getFromUserId()).plus(transfer.getAmount()));
            saldos.put(transfer.getToUserId(),
                    saldos.get(transfer.getToUserId()).minus(transfer.getAmount()));
        }
        List<String> pendientes = new ArrayList<>();
        for (Map.Entry<String, Money> entry : saldos.entrySet()) {
            if (!entry.getValue().isZero()) {
                pendientes.add(entry.getKey() + "=" + entry.getValue().format());
            }
        }
        assertTrue("Quedaron saldos sin saldar: " + pendientes, pendientes.isEmpty());
    }
}
