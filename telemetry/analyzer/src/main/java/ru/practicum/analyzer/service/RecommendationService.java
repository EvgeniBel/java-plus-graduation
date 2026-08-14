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

    private final UserActionRepository userRepository;
    private final EventSimilarityRepository similarityRepository;

    public List<Map.Entry<Long, Double>> getRecommendationsForUser(Long userId, int maxResults) {
        log.info("Рекомендации для пользователя: {}", userId);

        if (userId == null || userId <= 0) {
            log.warn("Невалидный userId: {}", userId);
            return Collections.emptyList();
        }

        List<Long> userEventIds = userRepository.findEventIdsByUserId(userId);
        if (userEventIds == null || userEventIds.isEmpty()) {
            log.info("У пользователя {} нет мероприятий", userId);
            return Collections.emptyList();
        }

        Map<Long, Double> scores = new HashMap<>();
        Set<Long> userEventSet = new HashSet<>(userEventIds);

        for (Long eventId : userEventIds) {
            List<EventSimilarity> similarities = similarityRepository.findSimilarByEventId(eventId);
            if (similarities == null || similarities.isEmpty()) {
                continue;
            }

            for (EventSimilarity sim : similarities) {
                if (sim == null || sim.getScore() == null || sim.getScore() <= 0) {
                    continue;
                }

                Long otherId = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();
                if (!userEventSet.contains(otherId) && otherId != null && otherId > 0) {
                    scores.merge(otherId, sim.getScore(), Double::sum);
                }
            }
        }

        if (scores.isEmpty()) {
            log.info("Не найдено рекомендаций для пользователя {}", userId);
            return Collections.emptyList();
        }

        // Исправлено: создаем effectively final переменную
        final int limit = maxResults > 0 ? maxResults : 10;

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

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

        // Исправлено: создаем effectively final переменные
        final Long targetEventId = eventId;
        final Set<Long> finalUserEventIds = userEventIds;
        final int limit = maxResults > 0 ? maxResults : 10;

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

    public Map<Long, Long> getInteractionsCount(List<Long> eventIds) {
        log.info("Количество взаимодействий для {} событий", eventIds != null ? eventIds.size() : 0);

        if (eventIds == null || eventIds.isEmpty()) {
            log.warn("Список eventIds пуст или null");
            return Collections.emptyMap();
        }

        Map<Long, Long> result = new HashMap<>();
        for (Long eventId : eventIds) {
            if (eventId != null && eventId > 0) {
                Long sum = userRepository.sumWeightByEventId(eventId);
                result.put(eventId, sum != null ? sum : 0L);
            }
        }
        return result;
    }
}