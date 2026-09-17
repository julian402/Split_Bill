package ue.edu.co.splitbill.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ue.edu.co.splitbill.domain.Balance;
import ue.edu.co.splitbill.domain.Transfer;

/**
 * Resultado completo de liquidar un grupo: los saldos, las transferencias propuestas y los nombres
 * necesarios para mostrarlos.
 *
 * El dominio trabaja solo con identificadores, porque el algoritmo no necesita saber como se llama
 * nadie. Los nombres se agregan aqui, en el borde entre la persistencia y la pantalla.
 */
public class SettlementResult {

    private final List<Balance> balances;
    private final List<Transfer> transfers;
    private final Map<String, String> userNames;
    private final int directTransferCount;

    public SettlementResult(List<Balance> balances, List<Transfer> transfers,
                            Map<String, String> userNames, int directTransferCount) {
        this.balances = Collections.unmodifiableList(balances);
        this.transfers = Collections.unmodifiableList(transfers);
        this.userNames = Collections.unmodifiableMap(new LinkedHashMap<>(userNames));
        this.directTransferCount = directTransferCount;
    }

    /**
     * Cuantas transferencias harian falta sin simplificar, es decir si cada participante le
     * devolviera su parte a quien pago, gasto por gasto. Es la cifra que el algoritmo reduce.
     */
    public int getDirectTransferCount() {
        return this.directTransferCount;
    }

    public List<Balance> getBalances() {
        return this.balances;
    }

    public List<Transfer> getTransfers() {
        return this.transfers;
    }

    /** Nombre del integrante, o un texto de respaldo si el integrante fue dado de baja. */
    public String getUserName(String userId) {
        String name = this.userNames.get(userId);
        return name == null ? "Integrante retirado" : name;
    }

    public boolean isSettled() {
        return this.transfers.isEmpty();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SettlementResult{");
        sb.append("balances=").append(balances.size());
        sb.append(", transfers=").append(transfers.size());
        sb.append('}');
        return sb.toString();
    }
}
