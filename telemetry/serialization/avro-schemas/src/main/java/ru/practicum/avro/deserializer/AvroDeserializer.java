package ru.practicum.avro.deserializer;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class AvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {

    private final DecoderFactory decoderFactory = DecoderFactory.get();
    private final Map<Class<?>, Schema> schemaCache = new ConcurrentHashMap<>();
    private BinaryDecoder decoder;
    private Class<T> targetClass;

    @Override
    @SuppressWarnings("unchecked")
    public void configure(Map<String, ?> configs, boolean isKey) {
        // Пытаемся получить класс из разных источников
        if (configs != null) {
            // 1. Из spring.deserializer.value.delegate.target.class
            if (configs.containsKey("spring.deserializer.value.delegate.target.class")) {
                try {
                    String className = configs.get("spring.deserializer.value.delegate.target.class").toString();
                    targetClass = (Class<T>) Class.forName(className);
                    log.debug("Target class set from spring.deserializer.value.delegate.target.class: {}", className);
                    return;
                } catch (ClassNotFoundException e) {
                    log.warn("Class not found: {}", configs.get("spring.deserializer.value.delegate.target.class"));
                }
            }

            // 2. Из value.deserializer.class
            if (configs.containsKey("value.deserializer.class")) {
                try {
                    String className = configs.get("value.deserializer.class").toString();
                    targetClass = (Class<T>) Class.forName(className);
                    log.debug("Target class set from value.deserializer.class: {}", className);
                    return;
                } catch (ClassNotFoundException e) {
                    log.warn("Class not found: {}", configs.get("value.deserializer.class"));
                }
            }
        }

        // 3. Fallback - определяем по топику
        // Это будет установлено в deserialize()
        log.warn("Target class not configured, will try to determine from topic");
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            log.warn("Получены null данные для топика: {}", topic);
            return null;
        }

        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            // Если targetClass еще не установлен, определяем по топику
            if (targetClass == null) {
                if ("user-actions-topic".equals(topic) || topic.endsWith("user-actions")) {
                    try {
                        targetClass = (Class<T>) Class.forName("ru.practicum.ewm.stats.avro.UserActionAvro");
                        log.info("Determined target class from topic {}: UserActionAvro", topic);
                    } catch (ClassNotFoundException e) {
                        throw new SerializationException("Cannot determine target class for topic: " + topic, e);
                    }
                } else if ("events-similarity-topic".equals(topic) || topic.endsWith("events-similarity")) {
                    try {
                        targetClass = (Class<T>) Class.forName("ru.practicum.ewm.stats.avro.EventSimilarityAvro");
                        log.info("Determined target class from topic {}: EventSimilarityAvro", topic);
                    } catch (ClassNotFoundException e) {
                        throw new SerializationException("Cannot determine target class for topic: " + topic, e);
                    }
                } else {
                    throw new SerializationException("Target class not configured and cannot be determined for topic: " + topic);
                }
            }

            Schema schema = getSchema(targetClass);
            DatumReader<T> reader = new SpecificDatumReader<>(schema);
            decoder = decoderFactory.binaryDecoder(in, decoder);

            T result = reader.read(null, decoder);
            log.debug("Успешная десериализация из топика: {}, класс: {}", topic, targetClass.getSimpleName());
            return result;

        } catch (IOException e) {
            log.error("Ошибка десериализации Avro из топика: {}", topic, e);
            throw new SerializationException(
                    String.format("Ошибка десериализации данных из топика '%s'.", topic), e
            );
        }
    }

    private Schema getSchema(Class<T> clazz) {
        return schemaCache.computeIfAbsent(clazz, c -> {
            try {
                return (Schema) c.getMethod("getClassSchema").invoke(null);
            } catch (Exception e) {
                throw new SerializationException("Ошибка получения схемы для класса: " + clazz, e);
            }
        });
    }

    @Override
    public void close() {
        log.debug("Закрытие Avro десериализатора");
    }
}