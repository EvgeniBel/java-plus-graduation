package ru.practicum.analyzer.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.model.UserMaxWeight;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.analyzer.service.SimilarityService;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final UserActionRepository userActionRepository;
    private final SimilarityService similarityService;

    @KafkaListener(topics = "${kafka.topics.user-actions}", groupId = "analyzer-group")
    public void consumeUserAction(UserActionAvro avro) {
        try {
            log.info("Получено действие пользователя: userId={}, eventId={}, action={}",
                    avro.getUserId(), avro.getEventId(), avro.getActionType());

            updateUserMaxWeight(avro);
            similarityService.updateSimilarity(avro.getUserId(), avro.getEventId());

        } catch (Exception e) {
            log.error("Ошибка обработки UserAction: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.events-similarity}", groupId = "analyzer-group")
    public void consumeEventSimilarity(EventSimilarityAvro avro) {
        try {
            log.info("Получено сходство мероприятий: ({}, {}) = {}",
                    avro.getEventA(), avro.getEventB(), avro.getScore());

            similarityService.saveSimilarity(avro);

        } catch (Exception e) {
            log.error("Ошибка обработки EventSimilarity: {}", e.getMessage(), e);
        }
    }

    private void updateUserMaxWeight(UserActionAvro avro) {
        Long userId = avro.getUserId();
        Long eventId = avro.getEventId();
        int weight = getActionWeight(avro.getActionType());

        UserMaxWeight existing = userActionRepository
                .findByUserIdAndEventId(userId, eventId)
                .orElse(null);

        if (existing == null) {
            UserMaxWeight maxWeight = UserMaxWeight.builder()
                    .userId(userId)
                    .eventId(eventId)
                    .maxWeight(weight)
                    .updatedAt(LocalDateTime.now())
                    .build();
            userActionRepository.save(maxWeight);
            log.info("Сохранён новый максимальный вес: userId={}, eventId={}, weight={}",
                    userId, eventId, weight);
        } else if (weight > existing.getMaxWeight()) {
            existing.setMaxWeight(weight);
            existing.setUpdatedAt(LocalDateTime.now());
            userActionRepository.save(existing);
            log.info("Обновлён максимальный вес: userId={}, eventId={}, weight={}",
                    userId, eventId, weight);
        } else {
            log.debug("Вес не изменился: userId={}, eventId={}, current={}, new={}",
                    userId, eventId, existing.getMaxWeight(), weight);
        }
    }

    private int getActionWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 1;
            case REGISTER -> 2;
            case LIKE -> 5;
        };
    }
}