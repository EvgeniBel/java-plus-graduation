package ru.practicum.aggregator.kafka;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.avro.deserializer.UserActionDeserializer;
import ru.practicum.avro.serializer.AvroSerializer;
import ru.practicum.ewm.stats.avro.UserActionAvro;


import java.util.Properties;

@Getter
@Setter
@Component
@RequiredArgsConstructor
public class KafkaClientConfigurationImpl implements ClientConfiguration {
    private Consumer<String, UserActionAvro> consumer;
    private Producer<String, SpecificRecordBase> producer;

    @Value("${kafka.bootstrap-servers:localhost:9092}")  // ← Исправлено
    private String bootstrapServers;

    @Value("${kafka.consumer.group-id:default-group}")
    private String groupId;

    @Value("${kafka.consumer.client.id:${kafka.consumer.group-id}}")  // ← Исправлено
    private String clientId;

    @Value("${kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    @Value("${kafka.consumer.enable-auto-commit:false}")
    private boolean enableAutoCommit;

    @Value("${kafka.producer.acks:all}")
    private String acks;

    @Value("${kafka.producer.retries:3}")
    private int retries;

    @Value("${kafka.producer.linger.ms:100}")
    private int lingerMs;

    @Value("${kafka.producer.delivery.timeout.ms:120000}")
    private int deliveryTimeoutMs;

    @Value("${kafka.producer.request.timeout.ms:30000}")
    private int requestTimeoutMs;

    @Value("${kafka.producer.max.in.flight.requests.per.connection:1}")
    private int maxInFlightRequests;

    @Value("${kafka.producer.enable.idempotence:true}")
    private boolean enableIdempotence;

    @Value("${kafka.producer.compression.type:snappy}")
    private String compressionType;

    public Consumer<String, UserActionAvro> getConsumer() {
        if (consumer == null) {
            initConsumer();
        }
        return consumer;
    }

    public Producer<String, SpecificRecordBase> getProducer() {
        if (producer == null) {
            initProducer();
        }
        return producer;
    }

    public void stop() {
        if (consumer != null) {
            consumer.close();
        }

        if (producer != null) {
            producer.close();
        }
    }

    private void initConsumer() {
        Properties config = new Properties();

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionDeserializer.class.getName());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);

        consumer = new KafkaConsumer<>(config);
    }

    private void initProducer() {
        Properties config = new Properties();

        // Основные настройки
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroSerializer.class.getName());

        // Настройки надежности
        config.put(ProducerConfig.ACKS_CONFIG, acks);
        config.put(ProducerConfig.RETRIES_CONFIG, retries);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, enableIdempotence);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, maxInFlightRequests);

        // Настройки задержки и таймаутов
        config.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, deliveryTimeoutMs);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);

        producer = new KafkaProducer<>(config);
    }
}
