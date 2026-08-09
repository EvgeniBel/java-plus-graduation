package ru.practicum.aggregator.util;

import ru.practicum.telemetry.messages.ActionType;

public class ActionTypeUtils {

    public static int getWeight(ActionType actionType) {
        return switch (actionType) {
            case ACTION_VIEW -> 1;
            case ACTION_REGISTER -> 2;
            case ACTION_LIKE -> 5;
            case ACTION_UNKNOWN -> 0;
            default -> 0;
        };
    }
}