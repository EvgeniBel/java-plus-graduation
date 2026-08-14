package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.aggregator.kafka.KafkaProducerService;
import ru.practicum.aggregator.model.UserActionEvent;
import ru.practicum.aggregator.util.ActionTypeUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregatorService {

    private static final double EPS = 1e-9;
    private final KafkaProducerService producer;
    // userId -> (eventId -> weight)
    private final Map<Long, Map<Long, Integer>> userWeights = new ConcurrentHashMap<>();
    // eventId -> totalWeight
    private final Map<Long, Double> eventTotal = new ConcurrentHashMap<>();
    // eventA -> (eventB -> minWeightSum)
    private final Map<Long, Map<Long, Double>> minSums = new ConcurrentHashMap<>();
    // Статистика для мониторинга
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
        int newWeight = ActionTypeUtils.getWeight(action.getActionType());
        int oldWeight = getWeight(userId, eventId);

        if (newWeight <= oldWeight) {
            log.debug("Вес не изменился: user={}, event={}, old={}, new={}",
                    userId, eventId, oldWeight, newWeight);
            skippedEvents.incrementAndGet();
            return;
        }

        // Обновляем вес
        userWeights.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .put(eventId, newWeight);
        eventTotal.merge(eventId, (double) (newWeight - oldWeight), Double::sum);

        // Пересчитываем сходство
        recalculate(eventId, userId, oldWeight, newWeight, action.getTimestamp());
        processedEvents.incrementAndGet();

        log.debug("Обработано действие: user={}, event={}, weight={}",
                userId, eventId, newWeight);
    }

    private int getWeight(Long userId, Long eventId) {
        Map<Long, Integer> weights = userWeights.get(userId);
        return weights == null ? 0 : weights.getOrDefault(eventId, 0);
    }

    private void recalculate(Long eventId, Long userId, int oldWeight, int newWeight, Long timestamp) {
        Map<Long, Integer> userEvents = userWeights.get(userId);
        if (userEvents == null || userEvents.isEmpty()) {
            return;
        }

        int updatedCount = 0;
        for (Map.Entry<Long, Integer> entry : userEvents.entrySet()) {
            Long otherId = entry.getKey();
            if (otherId.equals(eventId)) {
                continue;
            }

            int otherWeight = entry.getValue();
            updateMinSum(eventId, otherId, oldWeight, newWeight, otherWeight);

            double similarity = calcSimilarity(eventId, otherId);
            if (similarity > EPS) {
                producer.sendSimilarity(eventId, otherId, similarity, timestamp);
                updatedCount++;
            }
        }

        if (updatedCount > 0) {
            log.debug("Отправлено {} обновлений сходства для события {}", updatedCount, eventId);
        }
    }

    private void updateMinSum(Long a, Long b, int oldW, int newW, int otherW) {
        long first = Math.min(a, b);
        long second = Math.max(a, b);

        double delta = Math.min(newW, otherW) - Math.min(oldW, otherW);
        if (Math.abs(delta) < EPS) {
            return;
        }

        minSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>())
                .merge(second, delta, Double::sum);
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

        Double sA = eventTotal.get(a);
        Double sB = eventTotal.get(b);

        if (sA == null || sB == null || sA <= EPS || sB <= EPS) {
            return 0.0;
        }

        double similarity = sMin / (Math.sqrt(sA) * Math.sqrt(sB));
        return Math.min(1.0, similarity); // Нормализация
    }

    // Методы для мониторинга
    public Map<String, Long> getStats() {
        return Map.of(
                "processedEvents", processedEvents.get(),
                "skippedEvents", skippedEvents.get(),
                "usersTracked", (long) userWeights.size(),
                "eventsTracked", (long) eventTotal.size(),
                "similarityPairs", minSums.values().stream().mapToLong(Map::size).sum()
        );
    }

    public void clearCache() {
        userWeights.clear();
        eventTotal.clear();
        minSums.clear();
        processedEvents.set(0);
        skippedEvents.set(0);
        log.info("Кэш очищен");
    }
}