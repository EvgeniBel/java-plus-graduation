package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.model.UserMaxWeight;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarityService {

    private final EventSimilarityRepository similarityRepository;
    private final UserActionRepository userActionRepository;

    @Transactional
    public void updateSimilarity(Long userId, Long eventId) {
        log.info("Обновление сходства для события {} после действия пользователя {}", eventId, userId);

        // Получаем все мероприятия, с которыми взаимодействовал пользователь
        List<Long> userEventIds = userActionRepository.findEventIdsByUserId(userId);

        for (Long otherEventId : userEventIds) {
            if (otherEventId.equals(eventId)) continue;

            // Рассчитываем новое сходство
            double newSimilarity = calculateSimilarity(eventId, otherEventId);

            // Сохраняем в БД
            saveSimilarityPair(eventId, otherEventId, newSimilarity);
        }
    }

    @Transactional
    public void saveSimilarity(EventSimilarityAvro avro) {
        long eventA = Math.min(avro.getEventA(), avro.getEventB());
        long eventB = Math.max(avro.getEventA(), avro.getEventB());

        saveSimilarityPair(eventA, eventB, avro.getScore());
    }

    private void saveSimilarityPair(Long eventA, Long eventB, Double score) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        Optional<EventSimilarity> existing = similarityRepository.findByEventPair(first, second);

        if (existing.isPresent()) {
            EventSimilarity similarity = existing.get();
            similarity.setSimilarityScore(score);
            similarity.setCalculatedAt(LocalDateTime.now());
            similarityRepository.save(similarity);
            log.debug("Обновлено сходство: ({}, {}) = {}", first, second, score);
        } else {
            EventSimilarity similarity = EventSimilarity.builder()
                    .eventAId(first)
                    .eventBId(second)
                    .similarityScore(score)
                    .calculatedAt(LocalDateTime.now())
                    .build();
            similarityRepository.save(similarity);
            log.debug("Сохранено новое сходство: ({}, {}) = {}", first, second, score);
        }
    }

    private double calculateSimilarity(Long eventA, Long eventB) {
        // Получаем всех пользователей для обоих мероприятий
        List<UserMaxWeight> weightsA = userActionRepository.findByEventId(eventA);
        List<UserMaxWeight> weightsB = userActionRepository.findByEventId(eventB);

        if (weightsA.isEmpty() || weightsB.isEmpty()) {
            return 0.0;
        }

        // Считаем S_min (сумма минимальных весов)
        double sMin = 0.0;
        double sA = 0.0;
        double sB = 0.0;

        for (UserMaxWeight wA : weightsA) {
            sA += wA.getMaxWeight();
            for (UserMaxWeight wB : weightsB) {
                if (wA.getUserId().equals(wB.getUserId())) {
                    sMin += Math.min(wA.getMaxWeight(), wB.getMaxWeight());
                }
            }
        }

        for (UserMaxWeight wB : weightsB) {
            sB += wB.getMaxWeight();
        }

        if (sA == 0 || sB == 0) {
            return 0.0;
        }

        return sMin / (Math.sqrt(sA) * Math.sqrt(sB));
    }
}
