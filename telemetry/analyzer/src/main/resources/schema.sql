-- Таблица для хранения действий пользователей
CREATE TABLE IF NOT EXISTS user_actions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    weight INTEGER NOT NULL,
    timestamp BIGINT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, event_id)
);

-- Индексы для оптимизации запросов
CREATE INDEX IF NOT EXISTS idx_user_actions_user_id ON user_actions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_actions_event_id ON user_actions(event_id);
CREATE INDEX IF NOT EXISTS idx_user_actions_user_event ON user_actions(user_id, event_id);

-- Таблица для хранения сходства мероприятий
CREATE TABLE IF NOT EXISTS event_similarities (
    id BIGSERIAL PRIMARY KEY,
    event_a BIGINT NOT NULL,
    event_b BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    timestamp BIGINT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(event_a, event_b)
);

-- Индексы для оптимизации запросов
CREATE INDEX IF NOT EXISTS idx_event_similarities_event_a ON event_similarities(event_a);
CREATE INDEX IF NOT EXISTS idx_event_similarities_event_b ON event_similarities(event_b);
CREATE INDEX IF NOT EXISTS idx_event_similarities_score ON event_similarities(score DESC);