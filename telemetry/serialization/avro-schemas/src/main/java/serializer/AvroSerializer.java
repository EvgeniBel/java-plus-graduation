package serializer;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Slf4j
public class AvroSerializer<T extends SpecificRecordBase> implements Serializer<T> {

    private final EncoderFactory encoderFactory = EncoderFactory.get();
    private BinaryEncoder encoder;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        log.debug("Инициализация Avro сериализатора");
    }

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) {
            log.warn("Попытка сериализации null для топика: {}", topic);
            return null;
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            DatumWriter<T> writer = new SpecificDatumWriter<>(data.getSchema());
            encoder = encoderFactory.binaryEncoder(out, encoder);
            writer.write(data, encoder);
            encoder.flush();

            byte[] result = out.toByteArray();
            log.debug("Успешная сериализация для топика: {}, размер: {} байт", topic, result.length);
            return result;

        } catch (IOException e) {
            log.error("Ошибка сериализации Avro для топика: {}", topic, e);
            throw new SerializationException(
                    String.format("Ошибка сериализации данных для топика '%s'.", topic), e
            );
        }
    }

    @Override
    public void close() {
        log.debug("Закрытие Avro сериализатора");
    }
}