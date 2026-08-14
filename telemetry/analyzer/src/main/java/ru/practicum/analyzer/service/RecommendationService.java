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

    // Веса для разных типов действий
    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.2;

    private static final int DEFAULT_MAX_RESULTS = 10;
    private static final int DEFAULT_K_NEIGHBORS = 5;

    /**
     * Метод для получения рекомендаций на основе предсказания оценки
     */
    public List<Map.Entry<Long, Double>> getRecommendationsForUser(Long userId, int maxResults) {
        log.info("Рекомендации для пользователя: {}", userId);

        if (userId == null || userId <= 0) {
            log.warn("Невалидный userId: {}", userId);
            return Collections.emptyList();
        }

        // 1. Получаем последние N действий пользователя
        int limit = maxResults > 0 ? maxResults : DEFAULT_MAX_RESULTS;
        List<UserAction> userActions = userRepository.findLastUserActions(userId, PageRequest.of(0, limit));

        if (userActions == null || userActions.isEmpty()) {
            log.info("У пользователя {} нет действий", userId);
            return Collections.emptyList();
        }

        // Получаем ID мероприятий, с которыми пользователь уже взаимодействовал
        Set<Long> interactedEvents = userActions.stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        // 2. Находим похожие мероприятия для каждого действия пользователя
        Map<Long, Double> candidateScores = new HashMap<>();

        for (UserAction action : userActions) {
            Long eventId = action.getEventId();
            // Получаем вес действия пользователя
            double userWeight = getActionWeight(action.getActionType());

            List<EventSimilarity> similarities = similarityRepository.findSimilarByEventId(eventId);
            if (similarities == null || similarities.isEmpty()) {
                continue;
            }

            for (EventSimilarity sim : similarities) {
                if (sim == null || sim.getScore() == null || sim.getScore() <= 0) {
                    continue;
                }

                Long otherId = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();
                // Исключаем мероприятия, с которыми пользователь уже взаимодействовал
                if (!interactedEvents.contains(otherId) && otherId != null && otherId > 0) {
                    // Учитываем вес действия пользователя при расчете оценки
                    double weightedScore = sim.getScore() * userWeight;
                    candidateScores.merge(otherId, weightedScore, Double::sum);
                }
            }
        }

        if (candidateScores.isEmpty()) {
            log.info("Не найдено рекомендаций для пользователя {}", userId);
            return Collections.emptyList();
        }

        // 3. Для каждого кандидата вычисляем предсказанную оценку
        Map<Long, Double> predictedScores = new HashMap<>();
        for (Map.Entry<Long, Double> entry : candidateScores.entrySet()) {
            Long candidateEventId = entry.getKey();
            double predictedScore = predictScore(userId, candidateEventId);
            predictedScores.put(candidateEventId, predictedScore);
        }

        // 4. Сортируем по убыванию оценки и возвращаем
        final int resultLimit = maxResults > 0 ? maxResults : DEFAULT_MAX_RESULTS;

        return predictedScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(resultLimit)
                .collect(Collectors.toList());
    }

    /**
     * Предсказание оценки для мероприятия на основе K ближайших соседей
     */
    private double predictScore(Long userId, Long targetEventId) {
        // 1. Получаем все мероприятия, с которыми пользователь взаимодействовал
        List<Long> userEventIds = userRepository.findEventIdsByUserId(userId);
        if (userEventIds == null || userEventIds.isEmpty()) {
            return 0.0;
        }

        // 2. Находим K ближайших соседей (мероприятий, похожих на target)
        List<EventSimilarity> similarities = similarityRepository.findSimilarByEventId(targetEventId);
        if (similarities == null || similarities.isEmpty()) {
            return 0.0;
        }

        // 3. Фильтруем только те мероприятия, с которыми пользователь взаимодействовал
        Map<Long, Double> neighborScores = new HashMap<>();
        for (EventSimilarity sim : similarities) {
            if (sim == null || sim.getScore() == null || sim.getScore() <= 0) {
                continue;
            }

            Long otherId = sim.getEventA().equals(targetEventId) ? sim.getEventB() : sim.getEventA();
            if (userEventIds.contains(otherId)) {
                // Получаем вес действия пользователя для этого мероприятия
                Double userActionWeight = getUserActionWeight(userId, otherId);
                if (userActionWeight != null && userActionWeight > 0) {
                    neighborScores.put(otherId, sim.getScore() * userActionWeight);
                }
            }
        }

        if (neighborScores.isEmpty()) {
            return 0.0;
        }

        // 4. Сортируем соседей по сходству и берем K лучших
        final int k = Math.min(DEFAULT_K_NEIGHBORS, neighborScores.size());

        return neighborScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(k)
                .mapToDouble(entry -> {
                    // Получаем вес действия пользователя для этого мероприятия
                    Double weight = getUserActionWeight(userId, entry.getKey());
                    return weight != null ? weight * entry.getValue() : 0.0;
                })
                .sum() / k;
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
                // Получаем сумму максимальных весов для каждого пользователя
                Long sum = userRepository.sumMaxWeightsByEventId(eventId);
                result.put(eventId, sum != null ? sum.doubleValue() : 0.0);
            }
        }
        return result;
    }

    /**
     * Вспомогательные методы
     */
    private double getActionWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> VIEW_WEIGHT;
            case REGISTER -> REGISTER_WEIGHT;
            case LIKE -> LIKE_WEIGHT;
            default -> 0.0;
        };
    }

    private Double getUserActionWeight(Long userId, Long eventId) {
        Optional<UserAction> action = userRepository.findByUserIdAndEventId(userId, eventId);
        if (action.isPresent()) {
            return getActionWeight(action.get().getActionType());
        }
        return null;
    }
}