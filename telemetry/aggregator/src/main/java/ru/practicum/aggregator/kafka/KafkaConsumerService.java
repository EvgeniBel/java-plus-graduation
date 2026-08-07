package ru.practicum.aggregator.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    @KafkaListener(
            topics = "${kafka.topics.user-actions}",
            groupId = "aggregator-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUserAction(UserActionAvro action) {
        try {
            log.info("Получено действие из Kafka: userId={}, eventId={}, actionType={}",
                    action.getUserId(),
                    action.getEventId(),
                    action.getActionType());

            processUserAction(action);

        } catch (Exception e) {
            log.error("Ошибка обработки UserAction: {}", e.getMessage(), e);
        }
    }

    private void processUserAction(UserActionAvro action) {
        log.info("Обработка действия: userId={}, eventId={}",
                action.getUserId(), action.getEventId());
    }
}