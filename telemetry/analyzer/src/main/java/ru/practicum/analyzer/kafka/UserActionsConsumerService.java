package ru.practicum.analyzer.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.UserAction;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.analyzer.util.ActionTypeUtils;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionsConsumerService {

    private final UserActionRepository repository;

    @KafkaListener(
            topics = "${kafka.topics.user-actions}",
            containerFactory = "userActionKafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(UserActionAvro action) {
        try {
            log.info("Получено действие: userId={}, eventId={}, type={}",
                    action.getUserId(), action.getEventId(), action.getActionType());

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