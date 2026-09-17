package ue.edu.co.splitbill.domain.split;

import java.util.List;

import ue.edu.co.splitbill.domain.Share;
import ue.edu.co.splitbill.domain.SplitType;

/**
 * Contrato para repartir un gasto entre sus participantes.
 *
 * Cada forma de dividir es una implementacion distinta de esta interfaz. AddExpenseActivity no conoce
 * ninguna de ellas: le pide la estrategia a SplitStrategyFactory segun lo que el usuario escogio en el
 * Spinner y la invoca siempre igual. Agregar una cuarta forma de dividir no obliga a tocar la Activity,
 * que es el principio abierto/cerrado aplicado.
 *
 * Invariante que toda implementacion debe cumplir:
 * la suma de las partes devueltas es EXACTAMENTE igual al total del gasto, sin centavos perdidos.
 */
public interface SplitStrategy {

    /**
     * Reparte el total entre los participantes.
     *
     * @throws IllegalArgumentException si la peticion no es valida
     */
    List<Share> split(SplitRequest request);

    /**
     * Valida la peticion antes de repartir.
     *
     * @throws IllegalArgumentException si la peticion no es valida
     */
    void validar(SplitRequest request);

    /** Tipo de division que implementa esta estrategia. */
    SplitType getType();
}
