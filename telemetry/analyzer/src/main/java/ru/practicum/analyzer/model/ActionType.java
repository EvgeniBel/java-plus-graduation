package ru.practicum.analyzer.model;

public enum ActionType {
    VIEW(1),
    REGISTER(2),
    LIKE(5);

    private final int weight;

    ActionType(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
