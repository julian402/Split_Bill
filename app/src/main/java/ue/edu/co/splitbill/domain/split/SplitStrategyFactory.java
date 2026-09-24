package ue.edu.co.splitbill.domain.split;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import ue.edu.co.splitbill.domain.SplitType;

/**
 * Fabrica que entrega la estrategia correspondiente a un tipo de division.
 *
 * Es el unico punto del programa que conoce las clases concretas. La pantalla de registrar un gasto
 * solo sabe que tiene un SplitType y recibe un SplitStrategy, por lo que agregar una cuarta forma de
 * dividir se resuelve creando la clase nueva y registrandola aqui, sin tocar la interfaz de usuario.
 *
 * Las estrategias no guardan estado entre llamadas, asi que se reutiliza una sola instancia de cada una.
 */
public final class SplitStrategyFactory {

    private static final Map<SplitType, SplitStrategy> STRATEGIES = crearEstrategias();

    private SplitStrategyFactory() {
        //impide crear objetos de esta clase
    }

    private static Map<SplitType, SplitStrategy> crearEstrategias() {
        Map<SplitType, SplitStrategy> strategies = new EnumMap<>(SplitType.class);
        strategies.put(SplitType.EQUAL, new EqualSplitStrategy());
        strategies.put(SplitType.EXACT, new ExactAmountSplitStrategy());
        strategies.put(SplitType.PERCENTAGE, new PercentageSplitStrategy());
        return Collections.unmodifiableMap(strategies);
    }

    public static SplitStrategy create(SplitType type) {
        if (type == null) {
            throw new IllegalArgumentException("Debe indicar un tipo de división");
        }
        SplitStrategy strategy = STRATEGIES.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No hay una estrategia registrada para " + type);
        }
        return strategy;
    }
}
