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
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

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
        if (configs != null) {
            // Пытаемся получить класс из конфигурации
            if (configs.containsKey("spring.deserializer.value.delegate.target.class")) {
                try {
                    String className = configs.get("spring.deserializer.value.delegate.target.class").toString();
                    targetClass = (Class<T>) Class.forName(className);
                    log.info("Target class set from config: {}", className);
                    return;
                } catch (ClassNotFoundException e) {
                    log.warn("Class not found: {}", configs.get("spring.deserializer.value.delegate.target.class"));
                }
            }
        }
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
                if (topic.startsWith("stats.user-actions") || topic.contains("user-actions")) {
                    try {
                        targetClass = (Class<T>) Class.forName("ru.practicum.ewm.stats.avro.UserActionAvro");
                        log.info("Determined target class from topic {}: UserActionAvro", topic);
                    } catch (ClassNotFoundException e) {
                        throw new SerializationException("Cannot determine target class for topic: " + topic, e);
                    }
                } else if (topic.startsWith("stats.events-similarity") || topic.contains("events-similarity")) {
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