package ru.practicum.collector.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, UserActionAvro> kafkaTemplate;

    @Value("${kafka.topics.user-actions}")
    private String userActionsTopic;

    public void sendUserAction(UserActionAvro action) {
        try {
            String key = String.valueOf(action.getUserId());

            log.info("Отправка действия в Kafka: topic={}, userId={}, eventId={}, action={}",
                    userActionsTopic, action.getUserId(), action.getEventId(), action.getActionType());

            CompletableFuture<SendResult<String, UserActionAvro>> future =
                    kafkaTemplate.send(userActionsTopic, key, action);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Успешно отправлено: topic={}, partition={}, offset={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
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