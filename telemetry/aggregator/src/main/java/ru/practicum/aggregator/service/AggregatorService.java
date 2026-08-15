package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.aggregator.kafka.KafkaProducerService;
import ru.practicum.aggregator.model.UserActionEvent;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregatorService {

    private static final double EPS = 1e-9;
    private final KafkaProducerService producer;

    // Веса ДОЛЖНЫ совпадать с тестером
    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;

    // eventId -> (userId -> weight)
    private final Map<Long, Map<Long, Double>> eventUserWeights = new ConcurrentHashMap<>();

    // eventId -> totalWeight (СУММА ВЕСОВ, а не квадратов!) - как в тестере
    private final Map<Long, Double> eventTotal = new ConcurrentHashMap<>();

    // eventA -> (eventB -> S_min) - сумма минимальных весов для пары
    private final Map<Long, Map<Long, Double>> minSums = new ConcurrentHashMap<>();

    private final AtomicLong processedEvents = new AtomicLong(0);
    private final AtomicLong skippedEvents = new AtomicLong(0);

    public synchronized void process(UserActionEvent action) {
        if (action == null || !action.isValid()) {
            log.warn("Получено невалидное действие: {}", action);
            skippedEvents.incrementAndGet();
            return;
        }

        Long userId = action.getUserId();
        Long eventId = action.getEventId();
        double newWeight = getWeight(action.getActionType());

        // Получаем текущий вес пользователя для этого мероприятия
        double oldWeight = getWeight(eventId, userId);

        // Если вес не изменился - пропускаем
        if (newWeight <= oldWeight + EPS) {
            log.debug("Вес не изменился: user={}, event={}, old={}, new={}",
                    userId, eventId, oldWeight, newWeight);
            skippedEvents.incrementAndGet();
            return;
        }

        // 1. Обновляем вес пользователя для мероприятия
        Map<Long, Double> userWeights = eventUserWeights.computeIfAbsent(eventId,
                k -> new ConcurrentHashMap<>());

        Double previousWeight = userWeights.get(userId);
        if (previousWeight != null) {
            // Обновляем сумму весов (не квадратов!) - как в тестере
            eventTotal.merge(eventId, newWeight - previousWeight, Double::sum);
        } else {
            // Новый пользователь - добавляем вес
            eventTotal.merge(eventId, newWeight, Double::sum);
        }

        // Сохраняем новый вес
        userWeights.put(userId, newWeight);

        // 2. Пересчитываем сходство с другими мероприятиями
        recalculateSimilarities(eventId, userId, oldWeight, newWeight, action.getTimestamp());

        processedEvents.incrementAndGet();
        log.debug("Обработано действие: user={}, event={}, weight={}",
                userId, eventId, newWeight);
    }

    private double getWeight(Long eventId, Long userId) {
        Map<Long, Double> weights = eventUserWeights.get(eventId);
        return weights == null ? 0.0 : weights.getOrDefault(userId, 0.0);
    }

    private double getWeight(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> VIEW_WEIGHT;
            case REGISTER -> REGISTER_WEIGHT;
            case LIKE -> LIKE_WEIGHT;
            default -> 0.0;
        };
    }

    private void recalculateSimilarities(Long eventId, Long userId, double oldWeight, double newWeight, Long timestamp) {
        // Получаем все мероприятия, с которыми взаимодействовал пользователь
        Map<Long, Double> userEvents = getUserEvents(userId);
        if (userEvents == null || userEvents.isEmpty()) {
            return;
        }

        int updatedCount = 0;
        for (Map.Entry<Long, Double> entry : userEvents.entrySet()) {
            Long otherEventId = entry.getKey();
            if (otherEventId.equals(eventId)) {
                continue;
            }

            double otherWeight = entry.getValue();

            // Обновляем S_min для пары (eventId, otherEventId)
            updateMinSum(eventId, otherEventId, oldWeight, newWeight, otherWeight);

            // Вычисляем новое сходство
            double similarity = calcSimilarity(eventId, otherEventId);
            if (similarity > EPS && similarity <= 1.0) {
                // Округляем до 2 знаков как в тестере
                double rounded = Math.round(similarity * 100.0) / 100.0;
                producer.sendSimilarity(eventId, otherEventId, rounded, timestamp);
                updatedCount++;
                log.debug("Отправлено сходство: eventA={}, eventB={}, score={}",
                        Math.min(eventId, otherEventId),
                        Math.max(eventId, otherEventId),
                        rounded);
            }
        }

        if (updatedCount > 0) {
            log.debug("Отправлено {} обновлений сходства для события {}", updatedCount, eventId);
        }
    }

    private Map<Long, Double> getUserEvents(Long userId) {
        Map<Long, Double> result = new ConcurrentHashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : eventUserWeights.entrySet()) {
            Double weight = entry.getValue().get(userId);
            if (weight != null && weight > EPS) {
                result.put(entry.getKey(), weight);
            }
        }
        return result;
    }

    private void updateMinSum(Long a, Long b, double oldW, double newW, double otherW) {
        long first = Math.min(a, b);
        long second = Math.max(a, b);

        double oldMin = Math.min(oldW, otherW);
        double newMin = Math.min(newW, otherW);
        double delta = newMin - oldMin;

        if (Math.abs(delta) < EPS) {
            return;
        }

        minSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>())
                .merge(second, delta, Double::sum);

        log.debug("Обновлен S_min для пары ({}, {}): delta={}", first, second, delta);
    }

    private double calcSimilarity(Long a, Long b) {
        long first = Math.min(a, b);
        long second = Math.max(a, b);

        Map<Long, Double> firstMap = minSums.get(first);
        if (firstMap == null) {
            return 0.0;
        }
        Double sMin = firstMap.get(second);
        if (sMin == null || sMin <= EPS) {
            return 0.0;
        }

        // Используем СУММУ ВЕСОВ (не квадратов!) - как в тестере
        Double sA = eventTotal.get(a);
        Double sB = eventTotal.get(b);

        if (sA == null || sB == null || sA <= EPS || sB <= EPS) {
            return 0.0;
        }

        double similarity = sMin / (Math.sqrt(sA) * Math.sqrt(sB));
        return Math.min(1.0, Math.max(0.0, similarity));
    }

    // Метод для получения суммы максимальных весов (для Analyzer)
    public Map<Long, Double> getInteractionsCount(List<Long> eventIds) {
        Map<Long, Double> result = new HashMap<>();
        for (Long eventId : eventIds) {
            if (eventId != null && eventId > 0) {
                // Сумма максимальных весов для каждого пользователя
                double sum = 0.0;
                Map<Long, Double> weights = eventUserWeights.get(eventId);
                if (weights != null) {
                    for (Double w : weights.values()) {
                        sum += w;
                    }
                }
                result.put(eventId, sum);
            }
        }
        return result;
    }

    public Map<String, Long> getStats() {
        return Map.of(
                "processedEvents", processedEvents.get(),
                "skippedEvents", skippedEvents.get(),
                "eventsTracked", (long) eventUserWeights.size(),
                "similarityPairs", minSums.values().stream().mapToLong(Map::size).sum()
        );
    }

    public void clearCache() {
        eventUserWeights.clear();
        eventTotal.clear();
        minSums.clear();
        processedEvents.set(0);
        skippedEvents.set(0);
        log.info("Кэш очищен");
    }
}