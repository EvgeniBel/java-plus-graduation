package ru.practicum.constants;

public final class ApiConstants {

    private ApiConstants() {
    }

    // ==================== БАЗОВЫЕ ПУТИ ====================
    public static final String API_PREFIX = "";
    public static final String ADMIN_PREFIX = API_PREFIX + "/admin";
    public static final String USER_PREFIX = API_PREFIX + "/users";
    public static final String PUBLIC_PREFIX = API_PREFIX + "/public";
    public static final String INTERNAL_PREFIX = API_PREFIX + "/internal";
    public static final String CATEGORY_PREFIX = API_PREFIX + "/categories";
    public static final String COMPILATION_PREFIX = API_PREFIX + "/compilations";
    public static final String EVENTS_PREFIX = API_PREFIX + "/events";
    public static final String RECOMMENDATION_PREFIX = API_PREFIX + "/recommendations";
    // ==================== ПАРАМЕТРЫ ====================
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String FROM_PARAM = "from";
    public static final String SIZE_PARAM = "size";
    public static final String DEFAULT_FROM = "0";
    public static final String DEFAULT_SIZE = "20";

    // ==================== ПАРАМЕТРЫ ПУТЕЙ ====================
    public static final String USER_ID_PATH = "/{userId}";
    public static final String EVENT_ID_PATH = "/{eventId}";
    public static final String REQUEST_ID_PATH = "/{requestId}";
    public static final String CATEGORY_ID_PATH = "/{categoryId}";
    public static final String CATEGORY_ID = "/{catId}";
    public static final String COMPILATION_ID_PATH = "/{compId}";
    public static final String COMMENT_ID_PATH = "/{commentId}";


    // ==================== ИМЕНА ПАРАМЕТРОВ ====================
    public static final String USER_BY_ID_PARAM = "userId";
    public static final String EVENT_BY_ID_PARAM = "eventId";
    public static final String COMMENT_BY_ID_PARAM = "commentId";
    public static final String REQUEST_BY_ID_PARAM = "requestId";


    // ==================== ПОЛЬЗОВАТЕЛИ ====================
    public static final String USERS_BASE = ADMIN_PREFIX + USER_PREFIX;
    public static final String USER_BY_ID = USERS_BASE + USER_ID_PATH;
    public static final String USERS_PUBLIC = PUBLIC_PREFIX + USER_PREFIX;
    public static final String USERS_INTERNAL = INTERNAL_PREFIX + USER_PREFIX;

    public static final String USER_EXISTS = USER_BY_ID + "/exists";
    public static final String USER_ID_EXISTS = USER_ID_PATH +"/exists";
    public static final String USER_EXISTS_INTERNAL = USERS_INTERNAL + USER_ID_PATH + "/exists";

    public static final String USER_SHORT_PUBLIC = PUBLIC_PREFIX + "/users" + USER_ID_PATH + "/short";
    public static final String USERS_SEARCH_PUBLIC = PUBLIC_PREFIX + "/users/search";
    public static final String USERS_BY_IDS = USERS_BASE + "/ids";
    public static final String USER_BY_ID_INTERNAL = USERS_INTERNAL + USER_ID_PATH;
    public static final String USER_VALIDATE_INTERNAL = USERS_INTERNAL + "/validate";

    // ==================== СОБЫТИЯ ====================
    public static final String EVENTS_BASE = ADMIN_PREFIX + "/events";
    public static final String EVENTS_PUBLIC = PUBLIC_PREFIX + "/events";
    public static final String EVENTS_USER = USER_PREFIX + "/{userId}/events";
    public static final String EVENTS_INTERNAL = INTERNAL_PREFIX + "/events";

    public static final String EVENT_BY_ID = EVENTS_BASE + EVENT_ID_PATH;
    public static final String EVENT_SHORT = EVENT_BY_ID + "/short";
    public static final String EVENT_FULL = EVENT_BY_ID + "/full";
    public static final String EVENT_STATUS = EVENT_BY_ID + "/status";
    public static final String EVENT_REQUESTS = EVENT_BY_ID + "/requests";
    public static final String EVENT_REQUESTS_STATUS = EVENT_REQUESTS + "/status";
    public static final String EVENT_CONFIRM_REQUEST = EVENT_BY_ID + "/confirm-request";
    public static final String EVENT_ID_REQUESTS = EVENT_ID_PATH + "/requests";
    public static final String EVENT_LIKE ="/{eventId}/like";

    public static final String EVENT_EXISTS = EVENT_BY_ID + "/exists";
    public static final String EVENT_EXISTS_INTERNAL = EVENTS_INTERNAL + EVENT_ID_PATH + "/exists";

    // ==================== КОММЕНТАРИИ ====================
    public static final String COMMENTS_BASE = ADMIN_PREFIX + "/comments";
    public static final String COMMENTS_USER = USER_PREFIX + "/comments";
    public static final String COMMENTS_PUBLIC = PUBLIC_PREFIX + "/comments";
    public static final String COMMENTS_INTERNAL = INTERNAL_PREFIX + "/comments";
    public static final String COMMENT_ID_PATH_MODERATE = COMMENT_ID_PATH + "/moderate";

    public static final String COMMENT_BY_ID = COMMENTS_BASE + COMMENT_ID_PATH;
    public static final String COMMENT_MODERATE = COMMENTS_BASE + COMMENT_ID_PATH + "/moderate";
    public static final String COMMENTS_BY_EVENT = "/events" + EVENT_ID_PATH + "/comments";
    public static final String COMMENTS_BY_USER = USER_PREFIX + USER_ID_PATH + "/comments";
    public static final String COMMENTS_ID_BY_USER_PATH = COMMENTS_USER + "/{commentId}";
    public static final String COMMENT_PUBLIC_BY_ID = "/comments" + COMMENT_ID_PATH;

    // Внутренние пути для комментариев
    public static final String COMMENT_EXISTS_INTERNAL = COMMENTS_INTERNAL + COMMENT_ID_PATH + "/exists";
    public static final String COMMENT_STATUS_INTERNAL = COMMENTS_INTERNAL + COMMENT_ID_PATH + "/status";

    // ==================== ЗАПРОСЫ НА УЧАСТИЕ ====================
    public static final String REQUESTS_BASE = "/users/{userId}/requests";
    public static final String REQUESTS_INTERNAL = INTERNAL_PREFIX + "/requests";
    public static final String REQUEST_BY_ID = REQUESTS_BASE + REQUEST_ID_PATH;
    public static final String REQUEST_CANCEL = REQUEST_BY_ID + "/cancel";
    public static final String REQUESTS_BY_EVENT = PUBLIC_PREFIX + "/events" + EVENT_ID_PATH + "/requests";
    public static final String REQUESTS_COUNT = REQUESTS_BY_EVENT + "/count";
    public static final String REQUEST_CREATE = REQUESTS_BASE;
    public static final String REQUEST_STATUS_UPDATE = REQUESTS_INTERNAL + REQUEST_ID_PATH + "/status";

    // ==================== КАТЕГОРИИ ====================
    public static final String CATEGORIES_BASE = ADMIN_PREFIX + "/categories";
    public static final String CATEGORIES_PUBLIC = PUBLIC_PREFIX + "/categories";
    public static final String CATEGORY_BY_ID = CATEGORIES_BASE + CATEGORY_ID_PATH;

    // ==================== ПОДБОРКИ ====================
    public static final String COMPILATIONS_BASE = ADMIN_PREFIX + "/compilations";
    public static final String COMPILATIONS_PUBLIC = PUBLIC_PREFIX + "/compilations";
    public static final String COMPILATION_BY_ID = COMPILATIONS_BASE + COMPILATION_ID_PATH;
}