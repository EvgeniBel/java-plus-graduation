package deserializer;

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
        if (configs != null && configs.containsKey("value.deserializer.class")) {
            try {
                String className = configs.get("value.deserializer.class").toString();
                targetClass = (Class<T>) Class.forName(className);
            } catch (ClassNotFoundException e) {
                log.error("Не найден класс для десериализации", e);
            }
        }
        log.debug("Инициализация Avro десериализатора для класса: {}", targetClass);
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            log.warn("Получены null данные для топика: {}", topic);
            return null;
        }

        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            if (targetClass == null) {
                throw new SerializationException("Target class not configured");
            }

            Schema schema = getSchema(targetClass);
            DatumReader<T> reader = new SpecificDatumReader<>(schema);
            decoder = decoderFactory.binaryDecoder(in, decoder);

            T result = reader.read(null, decoder);
            log.debug("Успешная десериализация из топика: {}, класс: {}, размер: {} байт",
                    topic, targetClass.getSimpleName(), data.length);
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