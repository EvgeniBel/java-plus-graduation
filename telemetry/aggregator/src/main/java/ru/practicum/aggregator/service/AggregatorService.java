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

    // eventId -> (userId -> weight) - ИСПРАВЛЕНО!
    private final Map<Long, Map<Long, Integer>> eventUserWeights = new ConcurrentHashMap<>();

    // eventId -> totalWeight (сумма весов всех пользователей для мероприятия)
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
        int newWeight = ActionTypeUtils.getWeight(action.getActionType());

        // Получаем текущий вес пользователя для этого мероприятия
        int oldWeight = getWeight(eventId, userId);

        // Если вес не изменился - пропускаем
        if (newWeight <= oldWeight) {
            log.debug("Вес не изменился: user={}, event={}, old={}, new={}",
                    userId, eventId, oldWeight, newWeight);
            skippedEvents.incrementAndGet();
            return;
        }

        // 1. Обновляем вес пользователя для мероприятия
        Map<Long, Integer> userWeights = eventUserWeights.computeIfAbsent(eventId,
                k -> new ConcurrentHashMap<>());

        // Если пользователь уже был в мапе, обновляем вес
        Integer previousWeight = userWeights.get(userId);
        if (previousWeight != null) {
            // Обновляем общую сумму (вычитаем старый вес, добавляем новый)
            eventTotal.merge(eventId, (double) (newWeight - previousWeight), Double::sum);
        } else {
            // Новый пользователь - добавляем вес
            eventTotal.merge(eventId, (double) newWeight, Double::sum);
        }

        // Сохраняем новый вес
        userWeights.put(userId, newWeight);

        // 2. Пересчитываем сходство с другими мероприятиями
        recalculateSimilarities(eventId, userId, oldWeight, newWeight, action.getTimestamp());

        processedEvents.incrementAndGet();
        log.debug("Обработано действие: user={}, event={}, weight={}",
                userId, eventId, newWeight);
    }

    private int getWeight(Long eventId, Long userId) {
        Map<Long, Integer> weights = eventUserWeights.get(eventId);
        return weights == null ? 0 : weights.getOrDefault(userId, 0);
    }

    private void recalculateSimilarities(Long eventId, Long userId, int oldWeight, int newWeight, Long timestamp) {
        // Получаем все мероприятия, с которыми взаимодействовал пользователь
        Map<Long, Integer> userEvents = getUserEvents(userId);
        if (userEvents == null || userEvents.isEmpty()) {
            return;
        }

        int updatedCount = 0;
        for (Map.Entry<Long, Integer> entry : userEvents.entrySet()) {
            Long otherEventId = entry.getKey();
            if (otherEventId.equals(eventId)) {
                continue;
            }

            int otherWeight = entry.getValue();

            // Обновляем S_min для пары (eventId, otherEventId)
            updateMinSum(eventId, otherEventId, oldWeight, newWeight, otherWeight);

            // Вычисляем новое сходство
            double similarity = calcSimilarity(eventId, otherEventId);
            if (similarity > EPS && similarity <= 1.0) {
                producer.sendSimilarity(eventId, otherEventId, similarity, timestamp);
                updatedCount++;
                log.debug("Отправлено сходство: eventA={}, eventB={}, score={}",
                        Math.min(eventId, otherEventId),
                        Math.max(eventId, otherEventId),
                        similarity);
            }
        }

        if (updatedCount > 0) {
            log.debug("Отправлено {} обновлений сходства для события {}", updatedCount, eventId);
        }
    }

    private Map<Long, Integer> getUserEvents(Long userId) {
        Map<Long, Integer> result = new ConcurrentHashMap<>();
        for (Map.Entry<Long, Map<Long, Integer>> entry : eventUserWeights.entrySet()) {
            Integer weight = entry.getValue().get(userId);
            if (weight != null && weight > 0) {
                result.put(entry.getKey(), weight);
            }
        }
        return result;
    }

    // ИСПРАВЛЕННЫЙ метод обновления S_min
    private void updateMinSum(Long a, Long b, int oldW, int newW, int otherW) {
        long first = Math.min(a, b);
        long second = Math.max(a, b);

        // Старое значение S_min
        double oldMin = Math.min(oldW, otherW);
        // Новое значение S_min
        double newMin = Math.min(newW, otherW);

        // Разница
        double delta = newMin - oldMin;
        if (Math.abs(delta) < EPS) {
            return;
        }

        // Обновляем сумму минимальных весов
        minSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>())
                .merge(second, delta, Double::sum);

        log.debug("Обновлен S_min для пары ({}, {}): delta={}", first, second, delta);
    }

    private double calcSimilarity(Long a, Long b) {
        long first = Math.min(a, b);
        long second = Math.max(a, b);

        // Получаем S_min
        Map<Long, Double> firstMap = minSums.get(first);
        if (firstMap == null) {
            return 0.0;
        }
        Double sMin = firstMap.get(second);
        if (sMin == null || sMin <= EPS) {
            return 0.0;
        }

        // Получаем общие суммы весов
        Double sA = eventTotal.get(a);
        Double sB = eventTotal.get(b);

        if (sA == null || sB == null || sA <= EPS || sB <= EPS) {
            return 0.0;
        }

        // Вычисляем косинусное сходство
        double similarity = sMin / (Math.sqrt(sA) * Math.sqrt(sB));

        // Нормализуем до [0, 1]
        return Math.min(1.0, Math.max(0.0, similarity));
    }

    // Методы для мониторинга и отладки
    public Map<String, Long> getStats() {
        return Map.of(
                "processedEvents", processedEvents.get(),
                "skippedEvents", skippedEvents.get(),
                "eventsTracked", (long) eventUserWeights.size(),
                "totalEventsWeight", (long) eventTotal.size(),
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