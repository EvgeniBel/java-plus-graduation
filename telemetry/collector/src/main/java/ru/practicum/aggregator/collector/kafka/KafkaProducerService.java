package ru.practicum.aggregator.collector.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, UserActionAvro> kafkaTemplate;

    @Value("${kafka.topics.user-actions}")
    private String userActionsTopic;

    public void sendUserAction(UserActionAvro action) {
        try {
            String key = action.getUserId().toString();

            log.info("Отправка действия в Kafka: topic={}, userId={}, eventId={}, action={}",
                    userActionsTopic, action.getUserId(), action.getEventId(), action.getActionType());

            kafkaTemplate.send(userActionsTopic, key, action)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Успешно отправлено: offset={}",
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Ошибка отправки в Kafka: {}", ex.getMessage(), ex);
                        }
                    });

        } catch (Exception e) {
            log.error("Критическая ошибка при отправке в Kafka: {}", e.getMessage(), e);
        }
    }
}