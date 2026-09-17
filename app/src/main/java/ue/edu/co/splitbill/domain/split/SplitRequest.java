package ue.edu.co.splitbill.domain.split;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ue.edu.co.splitbill.domain.Money;

/**
 * Datos de entrada para repartir un gasto.
 *
 * values solo se usa en las estrategias que lo necesitan:
 *   - EXACT      -> el monto que le corresponde a cada participante
 *   - PERCENTAGE -> el porcentaje que le corresponde a cada participante
 *   - EQUAL      -> se ignora, puede ir vacio
 */
public final class SplitRequest {

    private final Money total;
    private final List<String> participantIds;
    private final Map<String, BigDecimal> values;

    public SplitRequest(Money total, List<String> participantIds) {
        this(total, participantIds, Collections.<String, BigDecimal>emptyMap());
    }

    public SplitRequest(Money total, List<String> participantIds, Map<String, BigDecimal> values) {
        if (total == null) {
            throw new IllegalArgumentException("El gasto debe tener un monto");
        }
        if (participantIds == null || participantIds.isEmpty()) {
            throw new IllegalArgumentException("El gasto debe tener al menos un participante");
        }
        this.total = total;
        //Copias defensivas: una vez creada, la peticion no puede cambiar desde afuera
        this.participantIds = Collections.unmodifiableList(new ArrayList<>(participantIds));
        this.values = Collections.unmodifiableMap(
                new LinkedHashMap<>(values == null ? Collections.<String, BigDecimal>emptyMap() : values));
    }

    public Money getTotal() {
        return this.total;
    }

    public List<String> getParticipantIds() {
        return this.participantIds;
    }

    public Map<String, BigDecimal> getValues() {
        return this.values;
    }

    /** Valor asociado a un participante, o null si no fue digitado. */
    public BigDecimal getValue(String userId) {
        return this.values.get(userId);
    }

    public int getParticipantCount() {
        return this.participantIds.size();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SplitRequest{");
        sb.append("total=").append(total.toBigDecimal());
        sb.append(", participants=").append(participantIds.size());
        sb.append(", values=").append(values);
        sb.append('}');
        return sb.toString();
    }
}
