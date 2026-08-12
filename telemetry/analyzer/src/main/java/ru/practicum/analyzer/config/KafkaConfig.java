package ru.practicum.analyzer.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import ru.practicum.avro.deserializer.EventSimilarityDeserializer;
import ru.practicum.avro.deserializer.UserActionDeserializer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:analyzer-group}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, UserActionAvro> userActionConsumerFactory() {
        return createConsumerFactory(groupId + "-user-actions", UserActionDeserializer.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserActionAvro>
    userActionKafkaListenerContainerFactory() {
        return createContainerFactory(userActionConsumerFactory());
    }

    @Bean
    public ConsumerFactory<String, EventSimilarityAvro> eventSimilarityConsumerFactory() {
        return createConsumerFactory(groupId + "-similarity", EventSimilarityDeserializer.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventSimilarityAvro>
    eventSimilarityKafkaListenerContainerFactory() {
        return createContainerFactory(eventSimilarityConsumerFactory());
    }

    private <T> ConsumerFactory<String, T> createConsumerFactory(String groupId, Class<?> deserializer) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> createContainerFactory(
            ConsumerFactory<String, T> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, T> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(false);
        return factory;
    }
}