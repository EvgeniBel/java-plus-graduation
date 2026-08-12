package ru.practicum.aggregator.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.practicum.aggregator.model.UserActionEvent;
import ru.practicum.aggregator.service.AggregatorService;
import ru.practicum.aggregator.mapper.UserActionMapper;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final AggregatorService aggregatorService;
    private final UserActionMapper userActionMapper;

    @KafkaListener(
            topics = "${kafka.topics.user-actions}",
            groupId = "aggregator-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUserAction(
            @Payload UserActionAvro action,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.info("Получено действие из Kafka: userId={}, eventId={}, actionType={}, timestamp={}",
                    action.getUserId(),
                    action.getEventId(),
                    action.getActionType(),
                    action.getTimestamp());

            UserActionEvent event = userActionMapper.toModel(action);

            aggregatorService.processUserAction(event);

            acknowledgment.acknowledge();

            log.debug("Обработано сообщение: partition={}, offset={}", partition, offset);

        } catch (Exception e) {
            log.error("Ошибка обработки UserAction: {}", e.getMessage(), e);
        }
    }
}