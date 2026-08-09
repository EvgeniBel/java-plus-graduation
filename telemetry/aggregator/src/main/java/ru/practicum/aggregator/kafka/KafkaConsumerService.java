package ru.practicum.aggregator.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
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
    public void consumeUserAction(UserActionAvro action) {
        try {
            log.info("Получено действие из Kafka: userId={}, eventId={}, actionType={}",
                    action.getUserId(),
                    action.getEventId(),
                    action.getActionType());

            // Преобразуем Avro → модель
            UserActionEvent event = userActionMapper.toModel(action);

            // Обрабатываем в AggregatorService
            aggregatorService.processUserAction(event);

        } catch (Exception e) {
            log.error("Ошибка обработки UserAction: {}", e.getMessage(), e);
        }
    }
}