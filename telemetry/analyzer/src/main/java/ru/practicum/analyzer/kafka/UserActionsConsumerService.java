package ru.practicum.analyzer.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.UserAction;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.analyzer.util.ActionTypeUtils;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionsConsumerService {

    private final UserActionRepository userActionRepository;

    @KafkaListener(
            topics = "${kafka.topics.user-actions}",
            containerFactory = "userActionKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeUserAction(
            @Payload UserActionAvro action,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.info("Получено действие пользователя: userId={}, eventId={}, actionType={}, timestamp={}",
                    action.getUserId(), action.getEventId(), action.getActionType(), action.getTimestamp());

            // Проверяем, существует ли уже запись для этого пользователя и мероприятия
            var existingAction = userActionRepository.findByUserIdAndEventId(
                    action.getUserId(), action.getEventId());

            int newWeight = ActionTypeUtils.getWeight(action.getActionType());

            if (existingAction.isPresent()) {
                // Обновляем существующую запись, если новый вес больше
                UserAction userAction = existingAction.get();
                if (newWeight > userAction.getWeight()) {
                    userAction.setActionType(action.getActionType());
                    userAction.setWeight(newWeight);
                    userAction.setTimestamp(action.getTimestamp());
                    userAction.setUpdatedAt(Instant.now());
                    userActionRepository.save(userAction);
                    log.info("Обновлено действие пользователя: userId={}, eventId={}, newWeight={}",
                            action.getUserId(), action.getEventId(), newWeight);
                } else {
                    log.debug("Вес не изменился: userId={}, eventId={}, currentWeight={}, newWeight={}",
                            action.getUserId(), action.getEventId(), userAction.getWeight(), newWeight);
                }
            } else {
                // Создаем новую запись
                UserAction userAction = UserAction.builder()
                        .userId(action.getUserId())
                        .eventId(action.getEventId())
                        .actionType(action.getActionType())
                        .weight(newWeight)
                        .timestamp(action.getTimestamp())
                        .updatedAt(Instant.now())
                        .build();
                userActionRepository.save(userAction);
                log.info("Сохранено новое действие пользователя: userId={}, eventId={}",
                        action.getUserId(), action.getEventId());
            }

            acknowledgment.acknowledge();
            log.debug("Обработано сообщение: partition={}, offset={}", partition, offset);

        } catch (Exception e) {
            log.error("Ошибка обработки UserAction: {}", e.getMessage(), e);
            // Не подтверждаем, чтобы сообщение было обработано снова
        }
    }
}
