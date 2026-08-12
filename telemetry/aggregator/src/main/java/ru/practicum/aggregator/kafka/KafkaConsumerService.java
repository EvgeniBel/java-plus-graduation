package ru.practicum.aggregator.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.practicum.aggregator.mapper.UserActionMapper;
import ru.practicum.aggregator.service.AggregatorService;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final AggregatorService aggregatorService;
    private final UserActionMapper mapper;

    @KafkaListener(
            topics = "${kafka.topics.user-actions}",
            containerFactory = "listenerContainerFactory"
    )
    public void consume(UserActionAvro action) {
        try {
            log.info("Получено действие: userId={}, eventId={}, type={}",
                    action.getUserId(), action.getEventId(), action.getActionType());
            aggregatorService.process(mapper.toModel(action));
        } catch (Exception e) {
            log.error("Ошибка обработки: {}", e.getMessage(), e);
        }
    }
}