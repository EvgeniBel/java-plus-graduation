-- Таблица для хранения действий пользователей
CREATE TABLE IF NOT EXISTS interactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    rating FLOAT NOT NULL,           -- В коде используется rating, а не weight
    timestamp TIMESTAMP NOT NULL,    -- В коде используется LocalDateTime
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, event_id)
);

-- Индексы для оптимизации запросов
CREATE INDEX IF NOT EXISTS idx_interactions_user_id ON interactions(user_id);
CREATE INDEX IF NOT EXISTS idx_interactions_event_id ON interactions(event_id);
CREATE INDEX IF NOT EXISTS idx_interactions_user_event ON interactions(user_id, event_id);

-- Таблица для хранения сходства мероприятий
CREATE TABLE IF NOT EXISTS similarities (
    id BIGSERIAL PRIMARY KEY,
    event1 BIGINT NOT NULL,          -- В коде используется event1
    event2 BIGINT NOT NULL,          -- В коде используется event2
    similarity DOUBLE PRECISION NOT NULL,  -- В коде используется similarity
    timestamp TIMESTAMP NOT NULL,    -- В коде используется LocalDateTime
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(event1, event2)
);

-- Индексы для оптимизации запросов
CREATE INDEX IF NOT EXISTS idx_similarities_event1 ON similarities(event1);
CREATE INDEX IF NOT EXISTS idx_similarities_event2 ON similarities(event2);
CREATE INDEX IF NOT EXISTS idx_similarities_score ON similarities(similarity DESC);