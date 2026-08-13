package ru.practicum.collector.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.practicum.collector.kafka.AvroKafkaConfig;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private static final String TOPIC_USER_ACTIONS = "stats.user-actions.v1";

    private final AvroKafkaConfig kafkaClient;

    /**z
     * Отправка сообщения о действии пользователя в Kafka
     */
    public void sendUserAction(UserActionAvro userActionAvro) {
        try {
            Producer<String, SpecificRecordBase> producer = kafkaClient.getProducer();

            producer.send(new ProducerRecord<>(TOPIC_USER_ACTIONS, userActionAvro));

            log.info("Отправлено действие пользователя в Kafka: userId={}, eventId={}, actionType={}",
                    userActionAvro.getUserId(),
                    userActionAvro.getEventId(),
                    userActionAvro.getActionType());

        } catch (Exception e) {
            log.error("Ошибка отправки действия пользователя в Kafka", e);
            throw new RuntimeException("Failed to send user action to Kafka", e);
        }
    }
}
