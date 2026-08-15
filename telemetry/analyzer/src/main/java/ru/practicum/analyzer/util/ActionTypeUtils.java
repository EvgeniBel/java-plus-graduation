package ru.practicum.analyzer.util;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;

public final class ActionTypeUtils {

    private ActionTypeUtils() {
    }

    // Веса для тестера
    public static double getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
            default -> 0.0;
        };
    }
}