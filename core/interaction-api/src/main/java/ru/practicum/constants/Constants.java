package ru.practicum.constants;

import java.time.format.DateTimeFormatter;

public final class Constants {

    private Constants() {
    }

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
}
