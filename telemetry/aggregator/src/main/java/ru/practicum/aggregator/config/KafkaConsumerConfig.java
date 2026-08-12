package ru.practicum.aggregator.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import ru.practicum.avro.deserializer.EventSimilarityDeserializer;  // ← ИЗМЕНЕНО
import ru.practicum.avro.deserializer.UserActionDeserializer;      // ← ИЗМЕНЕНО
import ru.practicum.avro.serializer.AvroSerializer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import ru.practicum.avro.deserializer.UserActionDeserializer;  // ← Импорт
import ru.practicum.avro.serializer.AvroSerializer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "kafka")
public class KafkaConsumerConfig {

    private String bootstrapServers;
    private Consumer consumer = new Consumer();
    private Producer producer = new Producer();

    @Getter
    @Setter
    public static class Consumer {
        private String groupId = "aggregator-group";
        private String keyDeserializer = StringDeserializer.class.getName();
        private String valueDeserializer = UserActionDeserializer.class.getName();  // ← ИЗМЕНЕНО
        private String autoOffsetReset = "earliest";
        private boolean enableAutoCommit = false;
        private int maxPollRecords = 100;
    }

    @Getter
    @Setter
    public static class Producer {
        private String keySerializer = StringSerializer.class.getName();
        private String valueSerializer = AvroSerializer.class.getName();
        private String acks = "all";
        private int retries = 3;
        private boolean enableIdempotence = true;
        private int batchSize = 16384;
        private int lingerMs = 10;
    }

    // ===== CONSUMER для UserActionAvro =====

    @Bean
    public ConsumerFactory<String, UserActionAvro> userActionConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, consumer.getGroupId() + "-user-actions");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionDeserializer.class.getName());  // ← ИЗМЕНЕНО
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserActionAvro>
    userActionKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, UserActionAvro> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(userActionConsumerFactory());
        factory.setBatchListener(false);
        return factory;
    }

    // ===== PRODUCER для EventSimilarityAvro =====

    @Bean
    public ProducerFactory<String, EventSimilarityAvro> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, producer.getKeySerializer());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, producer.getValueSerializer());
        config.put(ProducerConfig.ACKS_CONFIG, producer.getAcks());
        config.put(ProducerConfig.RETRIES_CONFIG, producer.getRetries());
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, producer.isEnableIdempotence());
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, producer.getBatchSize());
        config.put(ProducerConfig.LINGER_MS_CONFIG, producer.getLingerMs());

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate(
            ProducerFactory<String, EventSimilarityAvro> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}