package ru.practicum.aggregator.util;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;

public final class ActionTypeUtils {

    private ActionTypeUtils() {}

    public static int getWeight(ActionTypeAvro type) {
        return type == null ? 0 : switch (type) {
            case VIEW -> 1;
            case REGISTER -> 2;
            case LIKE -> 5;
            default -> 0;
        };
    }
}