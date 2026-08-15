package ru.practicum.aggregator.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import ru.practicum.aggregator.mapper.UserActionMapper;
import ru.practicum.aggregator.model.UserActionEvent;
import ru.practicum.aggregator.service.AggregatorService;
import ru.practicum.avro.deserializer.UserActionDeserializer;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final AggregatorService aggregatorService;
    private final UserActionMapper mapper;
    private final UserActionDeserializer deserializer = new UserActionDeserializer();

    @KafkaListener(
            topics = "${kafka.topics.user-actions:stats.user-actions.v1}",
            containerFactory = "listenerContainerFactory"
    )
    public void consume(byte[] data,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.OFFSET) long offset,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.RECEIVED_KEY) Long key,
                        Acknowledgment ack) {
        try {
            UserActionAvro action = deserializer.deserialize(topic, data);

            log.info("Получено действие: userId={}, eventId={}, type={}, key={}",
                    action.getUserId(), action.getEventId(), action.getActionType(), key);

            UserActionEvent event = mapper.toModel(action);
            if (event != null && event.isValid()) {
                aggregatorService.process(event);
                if (ack != null) {
                    ack.acknowledge();
                }
            } else {
                log.warn("Пропуск невалидного действия: {}", action);
                if (ack != null) {
                    ack.acknowledge();
                }
            }

        } catch (Exception e) {
            log.error("Ошибка обработки действия: offset={}, key={}, error={}",
                    offset, key, e.getMessage(), e);
            // НЕ подтверждаем offset при ошибке
        }
    }
}