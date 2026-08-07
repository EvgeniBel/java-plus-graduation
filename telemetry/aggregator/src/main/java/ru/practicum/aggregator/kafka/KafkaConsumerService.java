package ru.practicum.aggregator.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.practicum.aggregator.model.ActionType;
import ru.practicum.aggregator.model.UserActionEvent;
import ru.practicum.aggregator.service.AggregatorService;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final AggregatorService aggregatorService;

    @KafkaListener(topics = "${kafka.topics.user-actions}", groupId = "aggregator-group")
    public void consumeUserAction(ConsumerRecord<String, UserActionAvro> record) {
        try {
            UserActionAvro avro = record.value();

            log.info("📥 Получено сообщение из Kafka: userId={}, eventId={}, action={}",
                    avro.getUserId(), avro.getEventId(), avro.getActionType());

            UserActionEvent event = UserActionEvent.builder()
                    .userId(avro.getUserId())
                    .eventId(avro.getEventId())
                    .actionType(ActionType.valueOf(avro.getActionType().name()))
                    .timestamp(avro.getTimestamp())
                    .build();

            aggregatorService.processUserAction(event);

        } catch (Exception e) {
            log.error("Ошибка обработки сообщения из Kafka: {}", e.getMessage(), e);
        }
    }
}