package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.model.UserMaxWeight;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int DEFAULT_NEIGHBORS = 5;
    private static final int DEFAULT_RECENT_ACTIONS = 10;

    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository similarityRepository;

    public List<RecommendedEvent> getRecommendationsForUser(Long userId, int maxResults) {
        log.info("Генерация рекомендаций для пользователя: {}, maxResults={}", userId, maxResults);

        // Шаг 1: Получаем последние N взаимодействий пользователя
        List<UserMaxWeight> recentActions = userActionRepository.findRecentByUserId(userId);

        if (recentActions.isEmpty()) {
            log.info("У пользователя {} нет взаимодействий", userId);
            return Collections.emptyList();
        }

        // Шаг 2: Находим похожие мероприятия
        Set<Long> userEventIds = recentActions.stream()
                .map(UserMaxWeight::getEventId)
                .collect(Collectors.toSet());

        // Шаг 3: Для каждого просмотренного мероприятия ищем похожие
        Map<Long, Double> candidateScores = new HashMap<>();

        for (UserMaxWeight action : recentActions) {
            Long eventId = action.getEventId();
            List<EventSimilarity> similarities = similarityRepository.findByEventId(eventId);

            for (EventSimilarity sim : similarities) {
                Long similarEventId = sim.getEventAId().equals(eventId)
                        ? sim.getEventBId() : sim.getEventAId();

                // Исключаем уже просмотренные
                if (userEventIds.contains(similarEventId)) {
                    continue;
                }

                // Суммируем оценки
                candidateScores.merge(similarEventId, sim.getSimilarityScore(), Double::sum);
            }
        }

        if (candidateScores.isEmpty()) {
            log.info("Нет кандидатов для рекомендаций пользователю {}", userId);
            return Collections.emptyList();
        }

        // Шаг 4: Сортируем и выбираем топ N
        return candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(entry -> {
                    // Вычисляем предсказанную оценку
                    double predictedScore = predictScore(userId, entry.getKey(), entry.getValue());
                    return new RecommendedEvent(entry.getKey(), predictedScore);
                })
                .collect(Collectors.toList());
    }

    private double predictScore(Long userId, Long targetEventId, double baseScore) {
        // 1. Находим K ближайших соседей
        List<EventSimilarity> neighbors = similarityRepository.findByEventId(targetEventId)
                .stream()
                .sorted((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()))
                .limit(DEFAULT_NEIGHBORS)
                .collect(Collectors.toList());

        if (neighbors.isEmpty()) {
            return baseScore;
        }

        // 2. Получаем оценки пользователя для соседей
        List<Long> neighborIds = neighbors.stream()
                .map(sim -> sim.getEventAId().equals(targetEventId)
                        ? sim.getEventBId() : sim.getEventAId())
                .collect(Collectors.toList());

        List<Object[]> userWeights = userActionRepository.findUserWeightsForEvents(userId, neighborIds);
        Map<Long, Integer> userRatings = userWeights.stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> (Integer) arr[1]
                ));

        // 3. Вычисляем взвешенную сумму
        double weightedSum = 0.0;
        double similaritySum = 0.0;

        for (EventSimilarity neighbor : neighbors) {
            Long neighborId = neighbor.getEventAId().equals(targetEventId)
                    ? neighbor.getEventBId() : neighbor.getEventAId();

            Integer rating = userRatings.get(neighborId);
            if (rating != null) {
                weightedSum += neighbor.getSimilarityScore() * rating;
                similaritySum += neighbor.getSimilarityScore();
            }
        }

        if (similaritySum == 0) {
            return baseScore;
        }

        return weightedSum / similaritySum;
    }

    public List<RecommendedEvent> getSimilarEvents(Long eventId, Long userId, int maxResults) {
        log.info("Поиск похожих мероприятий: eventId={}, userId={}, maxResults={}",
                eventId, userId, maxResults);

        // 1. Получаем все похожие мероприятия
        List<EventSimilarity> similarities = similarityRepository.findByEventId(eventId);

        if (similarities.isEmpty()) {
            log.info("Нет похожих мероприятий для eventId={}", eventId);
            return Collections.emptyList();
        }

        // 2. Получаем просмотренные пользователем мероприятия
        List<Long> userEventIds = userActionRepository.findEventIdsByUserId(userId);

        // 3. Фильтруем и сортируем
        return similarities.stream()
                .map(sim -> {
                    Long similarEventId = sim.getEventAId().equals(eventId)
                            ? sim.getEventBId() : sim.getEventAId();
                    return Map.entry(similarEventId, sim.getSimilarityScore());
                })
                .filter(entry -> !userEventIds.contains(entry.getKey()))
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(entry -> new RecommendedEvent(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public List<RecommendedEvent> getInteractionsCount(List<Long> eventIds) {
        log.info("Получение суммы весов для {} мероприятий", eventIds.size());

        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Object[]> results = userActionRepository.sumWeightsByEvents(eventIds);

        return results.stream()
                .map(arr -> new RecommendedEvent(
                        (Long) arr[0],
                        ((Number) arr[1]).doubleValue()
                ))
                .collect(Collectors.toList());
    }

    public static class RecommendedEvent {
        private final Long eventId;
        private final Double score;

        public RecommendedEvent(Long eventId, Double score) {
            this.eventId = eventId;
            this.score = score;
        }

        public Long getEventId() {
            return eventId;
        }

        public Double getScore() {
            return score;
        }
    }
}