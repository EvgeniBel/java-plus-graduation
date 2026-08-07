-- Создание таблицы user_max_weights
CREATE TABLE IF NOT EXISTS user_max_weights (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    max_weight INTEGER NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_user_event UNIQUE (user_id, event_id)
);

-- Создание таблицы event_similarities
CREATE TABLE IF NOT EXISTS event_similarities (
    id BIGSERIAL PRIMARY KEY,
    event_a_id BIGINT NOT NULL,
    event_b_id BIGINT NOT NULL,
    similarity_score DOUBLE PRECISION NOT NULL,
    calculated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_event_pair UNIQUE (event_a_id, event_b_id)
);

-- Индексы для производительности
CREATE INDEX IF NOT EXISTS idx_user_max_weights_user_id ON user_max_weights(user_id);
CREATE INDEX IF NOT EXISTS idx_user_max_weights_event_id ON user_max_weights(event_id);
CREATE INDEX IF NOT EXISTS idx_user_max_weights_updated_at ON user_max_weights(updated_at);

CREATE INDEX IF NOT EXISTS idx_event_similarities_event_a_id ON event_similarities(event_a_id);
CREATE INDEX IF NOT EXISTS idx_event_similarities_event_b_id ON event_similarities(event_b_id);
CREATE INDEX IF NOT EXISTS idx_event_similarities_score ON event_similarities(similarity_score DESC);