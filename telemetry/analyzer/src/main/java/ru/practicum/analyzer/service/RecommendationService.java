package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    /**
     * Получение списка рекомендуемых мероприятий для пользователя
     */
    public List<Map.Entry<Long, Double>> getRecommendationsForUser(Long userId, int maxResults) {
        log.info("Получение рекомендаций для пользователя: userId={}, maxResults={}", userId, maxResults);

        // 1. Получаем все мероприятия, с которыми взаимодействовал пользователь
        List<Long> userEventIds = userActionRepository.findEventIdsByUserId(userId);
        if (userEventIds.isEmpty()) {
            log.warn("Пользователь {} не взаимодействовал ни с одним мероприятием", userId);
            return Collections.emptyList();
        }

        // 2. Получаем сходства для всех мероприятий пользователя
        Map<Long, Double> recommendations = new HashMap<>();
        for (Long eventId : userEventIds) {
            List<EventSimilarity> similarities = eventSimilarityRepository.findSimilarEventsByEventId(eventId);
            for (EventSimilarity similarity : similarities) {
                Long similarEventId = similarity.getEventA().equals(eventId)
                        ? similarity.getEventB()
                        : similarity.getEventA();

                // Исключаем мероприятия, с которыми пользователь уже взаимодействовал
                if (!userEventIds.contains(similarEventId)) {
                    // Суммируем оценки сходства (можно использовать среднее или максимум)
                    recommendations.merge(similarEventId, similarity.getScore(), Double::sum);
                }
            }
        }

        // 3. Сортируем по убыванию оценки и ограничиваем количество
        return recommendations.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    /**
     * Получение списка мероприятий, похожих на указанное
     */
    public List<Map.Entry<Long, Double>> getSimilarEvents(Long eventId, Long userId, int maxResults) {
        log.info("Получение похожих мероприятий: eventId={}, userId={}, maxResults={}", eventId, userId, maxResults);

        // 1. Получаем мероприятия, с которыми пользователь уже взаимодействовал
        List<Long> userEventIds = userId != null && userId > 0
                ? userActionRepository.findEventIdsByUserId(userId)
                : Collections.emptyList();

        // 2. Получаем сходства для указанного мероприятия
        List<EventSimilarity> similarities = eventSimilarityRepository.findSimilarEventsByEventId(eventId);

        // 3. Фильтруем и сортируем
        return similarities.stream()
                .filter(s -> {
                    Long similarEventId = s.getEventA().equals(eventId) ? s.getEventB() : s.getEventA();
                    return !userEventIds.contains(similarEventId);
                })
                .map(s -> {
                    Long similarEventId = s.getEventA().equals(eventId) ? s.getEventB() : s.getEventA();
                    return Map.entry(similarEventId, s.getScore());
                })
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    /**
     * Получение суммы взаимодействий для указанных мероприятий
     */
    public Map<Long, Long> getInteractionsCount(List<Long> eventIds) {
        log.info("Получение количества взаимодействий для событий: {}", eventIds);

        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Для каждого мероприятия получаем сумму весов
        Map<Long, Long> result = new HashMap<>();
        for (Long eventId : eventIds) {
            Long sum = userActionRepository.sumWeightsByEventIds(Collections.singletonList(eventId));
            result.put(eventId, sum != null ? sum : 0L);
        }

        return result;
    }
}