package ru.practicum.analyzer.util;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;

public class ActionTypeUtils {

    public static int getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 1;
            case REGISTER -> 2;
            case LIKE -> 5;
            default -> 0;
        };
    }
}
