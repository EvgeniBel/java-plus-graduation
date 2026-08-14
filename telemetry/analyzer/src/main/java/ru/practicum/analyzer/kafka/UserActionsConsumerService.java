package ru.practicum.analyzer.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.UserAction;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.analyzer.util.ActionTypeUtils;
import ru.practicum.avro.deserializer.UserActionDeserializer;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionsConsumerService {

    private final UserActionRepository repository;
    private final UserActionDeserializer deserializer = new UserActionDeserializer();

    @KafkaListener(
            topics = "${kafka.topics.user-actions}",
            containerFactory = "userActionKafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(Long key, byte[] data) {
        try {
            // Десериализуем Avro из байтов
            UserActionAvro action = deserializer.deserialize("stats.user-actions.v1", data);

            log.info("Получено действие: key={}, userId={}, eventId={}, type={}",
                    key, action.getUserId(), action.getEventId(), action.getActionType());

            int newWeight = ActionTypeUtils.getWeight(action.getActionType());

            UserAction entity = repository
                    .findByUserIdAndEventId(action.getUserId(), action.getEventId())
                    .map(existing -> {
                        if (newWeight > existing.getWeight()) {
                            existing.setActionType(action.getActionType());
                            existing.setWeight(newWeight);
                            existing.setTimestamp(action.getTimestamp());
                            log.info("Обновлено: userId={}, eventId={}, weight={}->{}",
                                    action.getUserId(), action.getEventId(),
                                    existing.getWeight(), newWeight);
                        }
                        return existing;
                    })
                    .orElseGet(() -> UserAction.builder()
                            .userId(action.getUserId())
                            .eventId(action.getEventId())
                            .actionType(action.getActionType())
                            .weight(newWeight)
                            .timestamp(action.getTimestamp())
                            .build());

            repository.save(entity);

        } catch (Exception e) {
            log.error("Ошибка обработки: {}", e.getMessage(), e);
        }
    }
}