package ru.practicum.constants;

import java.time.format.DateTimeFormatter;

public final class Constants {

    private Constants() {
    }

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== СТАТУСЫ (константы для бизнес-логики) ====================

    // Статусы комментариев
    public static final String COMMENT_STATUS_PENDING = "PENDING";
    public static final String COMMENT_STATUS_APPROVED = "APPROVED";
    public static final String COMMENT_STATUS_REJECTED = "REJECTED";

    // Статусы событий
    public static final String EVENT_STATUS_PENDING = "PENDING";
    public static final String EVENT_STATUS_PUBLISHED = "PUBLISHED";
    public static final String EVENT_STATUS_CANCELED = "CANCELED";
    public static final String EVENT_STATUS_REJECTED = "REJECTED";

    // Статусы запросов
    public static final String REQUEST_STATUS_PENDING = "PENDING";
    public static final String REQUEST_STATUS_CONFIRMED = "CONFIRMED";
    public static final String REQUEST_STATUS_REJECTED = "REJECTED";
    public static final String REQUEST_STATUS_CANCELED = "CANCELED";

    // ==================== ДЕЙСТВИЯ ДЛЯ ИЗМЕНЕНИЯ СТАТУСА ====================
    public static final String STATE_ACTION_PUBLISH = "PUBLISH";
    public static final String STATE_ACTION_REJECT = "REJECT";
    public static final String STATE_ACTION_CANCEL = "CANCEL";
    public static final String STATE_ACTION_SEND = "SEND";

    // ==================== СОРТИРОВКА ====================
    public static final String SORT_BY_EVENT_DATE = "EVENT_DATE";
    public static final String SORT_BY_VIEWS = "VIEWS";
}