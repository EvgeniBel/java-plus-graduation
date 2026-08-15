package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.model.UserAction;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserActionRepository userRepository;
    private final EventSimilarityRepository similarityRepository;

    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;

    private static final int DEFAULT_MAX_RESULTS = 10;
    private static final int DEFAULT_K_NEIGHBORS = 5;

    /**
     * Получение рекомендаций для пользователя
     */
    public List<Map.Entry<Long, Double>> getRecommendationsForUser(Long userId, int maxResults) {
        log.info("Рекомендации для пользователя: {}", userId);

        if (userId == null || userId <= 0) {
            log.warn("Невалидный userId: {}", userId);
            return Collections.emptyList();
        }

        // Получаем все мероприятия, с которыми пользователь взаимодействовал
        List<Long> interactedEvents = userRepository.findEventIdsByUserId(userId);
        if (interactedEvents == null || interactedEvents.isEmpty()) {
            log.info("У пользователя {} нет действий", userId);
            return Collections.emptyList();
        }
        Set<Long> interactedSet = new HashSet<>(interactedEvents);

        // Получаем все схожести для мероприятий пользователя
        Map<Long, Double> candidateScores = new HashMap<>();
        Map<Long, Double> similarityScores = new HashMap<>();

        for (Long eventId : interactedEvents) {
            List<EventSimilarity> similarities = similarityRepository.findSimilarByEventId(eventId);
            if (similarities == null || similarities.isEmpty()) {
                continue;
            }

            for (EventSimilarity sim : similarities) {
                if (sim == null || sim.getScore() == null || sim.getScore() <= 0) {
                    continue;
                }

                Long otherId = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();
                if (!interactedSet.contains(otherId) && otherId != null && otherId > 0) {
                    candidateScores.merge(otherId, sim.getScore(), Double::sum);
                    similarityScores.merge(otherId, sim.getScore(), Double::sum);
                }
            }
        }

        if (candidateScores.isEmpty()) {
            log.info("Не найдено рекомендаций для пользователя {}", userId);
            return Collections.emptyList();
        }

        // Вычисляем предсказанные оценки (взвешенное среднее)
        Map<Long, Double> predictedScores = new HashMap<>();
        for (Map.Entry<Long, Double> entry : candidateScores.entrySet()) {
            Long candidateId = entry.getKey();
            double totalSimilarity = similarityScores.getOrDefault(candidateId, 0.0);
            if (totalSimilarity > 0) {
                predictedScores.put(candidateId, entry.getValue() / totalSimilarity);
            }
        }

        final int limit = maxResults > 0 ? maxResults : DEFAULT_MAX_RESULTS;

        return predictedScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Получение похожих мероприятий
     */
    public List<Map.Entry<Long, Double>> getSimilarEvents(Long eventId, Long userId, int maxResults) {
        log.info("Похожие мероприятия: eventId={}, userId={}", eventId, userId);

        if (eventId == null || eventId <= 0) {
            log.warn("Невалидный eventId: {}", eventId);
            return Collections.emptyList();
        }

        Set<Long> userEventIds = Collections.emptySet();
        if (userId != null && userId > 0) {
            List<Long> events = userRepository.findEventIdsByUserId(userId);
            if (events != null && !events.isEmpty()) {
                userEventIds = new HashSet<>(events);
            }
        }

        List<EventSimilarity> similarities = similarityRepository.findSimilarByEventId(eventId);
        if (similarities == null || similarities.isEmpty()) {
            log.info("Не найдено похожих мероприятий для события {}", eventId);
            return Collections.emptyList();
        }

        final Long targetEventId = eventId;
        final Set<Long> finalUserEventIds = userEventIds;
        final int limit = maxResults > 0 ? maxResults : DEFAULT_MAX_RESULTS;

        return similarities.stream()
                .filter(sim -> sim != null && sim.getScore() != null && sim.getScore() > 0)
                .map(sim -> {
                    Long otherId = sim.getEventA().equals(targetEventId) ? sim.getEventB() : sim.getEventA();
                    return Map.entry(otherId, sim.getScore());
                })
                .filter(entry -> entry.getKey() != null && entry.getKey() > 0)
                .filter(entry -> !finalUserEventIds.contains(entry.getKey()))
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Получение суммы максимальных весов для каждого мероприятия
     * (исправлено в соответствии с заданием)
     */
    public Map<Long, Double> getInteractionsCount(List<Long> eventIds) {
        log.info("Количество взаимодействий для {} событий", eventIds != null ? eventIds.size() : 0);

        if (eventIds == null || eventIds.isEmpty()) {
            log.warn("Список eventIds пуст или null");
            return Collections.emptyMap();
        }

        Map<Long, Double> result = new HashMap<>();
        for (Long eventId : eventIds) {
            if (eventId != null && eventId > 0) {

                Double sum = userRepository.sumMaxWeightsByEventId(eventId);
                result.put(eventId, sum != null ? sum : 0.0);
            }
        }
        return result;
    }
}