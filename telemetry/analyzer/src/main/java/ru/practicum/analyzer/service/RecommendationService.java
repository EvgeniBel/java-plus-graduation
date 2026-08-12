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

        List<Long> userEventIds = userRepository.findEventIdsByUserId(userId);
        if (userEventIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Double> scores = new HashMap<>();
        Set<Long> userEventSet = new HashSet<>(userEventIds);

        for (Long eventId : userEventIds) {
            for (EventSimilarity sim : similarityRepository.findSimilarByEventId(eventId)) {
                Long otherId = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();
                if (!userEventSet.contains(otherId)) {
                    scores.merge(otherId, sim.getScore(), Double::sum);
                }
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    public List<Map.Entry<Long, Double>> getSimilarEvents(Long eventId, Long userId, int maxResults) {
        log.info("Похожие мероприятия: eventId={}, userId={}", eventId, userId);

        Set<Long> userEventIds = userId != null && userId > 0
                ? new HashSet<>(userRepository.findEventIdsByUserId(userId))
                : Collections.emptySet();

        return similarityRepository.findSimilarByEventId(eventId).stream()
                .filter(sim -> {
                    Long otherId = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();
                    return !userEventIds.contains(otherId);
                })
                .map(sim -> {
                    Long otherId = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();
                    return Map.entry(otherId, sim.getScore());
                })
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    public Map<Long, Long> getInteractionsCount(List<Long> eventIds) {
        log.info("Количество взаимодействий для {} событий", eventIds.size());

        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> result = new HashMap<>();
        for (Long eventId : eventIds) {
            result.put(eventId, userRepository.sumWeightByEventId(eventId));
        }
        return result;
    }
}