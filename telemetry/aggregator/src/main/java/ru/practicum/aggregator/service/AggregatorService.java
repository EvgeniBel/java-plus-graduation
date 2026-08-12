package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.aggregator.kafka.KafkaProducerService;
import ru.practicum.aggregator.model.UserActionEvent;
import ru.practicum.aggregator.util.ActionTypeUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregatorService {

    private final KafkaProducerService kafkaProducerService;

    // Хранилище максимальных весов пользователей для мероприятий
    private final Map<Long, Map<Long, Integer>> userEventWeights = new ConcurrentHashMap<>();

    // Хранилище общих сумм весов для мероприятий
    private final Map<Long, Double> totalWeights = new ConcurrentHashMap<>();

    // Хранилище сумм минимальных весов для пар мероприятий
    private final Map<Long, Map<Long, Double>> minWeightsSums = new ConcurrentHashMap<>();

    public synchronized void processUserAction(UserActionEvent action) {
        Long userId = action.getUserId();
        Long eventId = action.getEventId();
        Integer newWeight = ActionTypeUtils.getWeight(action.getActionType());
        Long timestamp = action.getTimestamp();

        log.info("Обработка действия: userId={}, eventId={}, weight={}, timestamp={}",
                userId, eventId, newWeight, timestamp);

        // 1. Получаем текущий максимальный вес пользователя для этого события
        Map<Long, Integer> eventWeights = userEventWeights.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());
        Integer oldMaxWeight = eventWeights.get(userId);

        // 2. Если вес не изменился → пересчёт не нужен
        if (oldMaxWeight != null && oldMaxWeight >= newWeight) {
            log.debug("Вес не изменился: userId={}, eventId={}, oldWeight={}, newWeight={}",
                    userId, eventId, oldMaxWeight, newWeight);
            return;
        }

        // 3. Обновляем максимальный вес
        eventWeights.put(userId, newWeight);

        // 4. Если это НОВОЕ мероприятие (не было весов)
        if (oldMaxWeight == null) {
            // Добавляем новый вес в totalWeights
            totalWeights.merge(eventId, (double) newWeight, Double::sum);

            // Считаем сходство с новым мероприятием
            calculateSimilarityForNewEvent(eventId, timestamp);
        } else {
            // 5. Если вес увеличился → обновляем частные суммы
            double weightDiff = newWeight - oldMaxWeight;

            // Обновляем totalWeights
            totalWeights.merge(eventId, weightDiff, Double::sum);

            // Обновляем minWeightsSums для всех пар с этим событием
            updateMinWeightsSums(eventId, userId, weightDiff);

            // Пересчитываем сходство для всех пар с этим событием
            recalculateSimilarityForEvent(eventId, timestamp);
        }
    }

    private void calculateSimilarityForNewEvent(Long newEventId, Long timestamp) {
        log.info("Расчёт сходства для нового мероприятия: {}", newEventId);

        // Получаем всех пользователей, взаимодействовавших с новым событием
        Map<Long, Integer> newEventWeights = userEventWeights.get(newEventId);
        if (newEventWeights == null || newEventWeights.isEmpty()) {
            return;
        }

        // Для каждого существующего мероприятия
        for (Long otherEventId : userEventWeights.keySet()) {
            if (otherEventId.equals(newEventId)) continue;

            // Считаем S_min для пары (newEventId, otherEventId)
            double sMin = calculateSMin(newEventId, otherEventId);

            // Сохраняем S_min
            putMinWeightSum(newEventId, otherEventId, sMin);

            // Рассчитываем сходство
            double similarity = calculateSimilarity(newEventId, otherEventId);

            // Отправляем результат в Kafka с timestamp из действия
            sendSimilarity(newEventId, otherEventId, similarity, timestamp);
        }
    }

    private void recalculateSimilarityForEvent(Long eventId, Long timestamp) {
        log.info("Пересчёт сходства для мероприятия: {}", eventId);

        // Для всех пар с этим событием
        Set<Long> allEvents = new HashSet<>(userEventWeights.keySet());
        for (Long otherEventId : allEvents) {
            if (otherEventId.equals(eventId)) continue;

            // Получаем S_min
            double sMin = getMinWeightSum(eventId, otherEventId);

            if (sMin <= 0) continue;

            // Рассчитываем сходство
            double similarity = calculateSimilarity(eventId, otherEventId);

            // Отправляем в Kafka с timestamp из действия
            sendSimilarity(eventId, otherEventId, similarity, timestamp);
        }
    }

    private void updateMinWeightsSums(Long eventId, Long userId, double weightDiff) {
        log.debug("Обновление S_min для события: {}, userId: {}, diff: {}", eventId, userId, weightDiff);

        // Для всех мероприятий, с которыми взаимодействовал этот пользователь
        for (Long otherEventId : userEventWeights.keySet()) {
            if (otherEventId.equals(eventId)) continue;

            Map<Long, Integer> otherEventWeights = userEventWeights.get(otherEventId);
            if (otherEventWeights == null || !otherEventWeights.containsKey(userId)) {
                continue;
            }

            // Получаем минимальный вес пользователя для пары мероприятий
            int weightForEvent = userEventWeights.get(eventId).get(userId);
            int weightForOther = otherEventWeights.get(userId);
            double minWeight = Math.min(weightForEvent, weightForOther);

            // Обновляем S_min
            double currentSMin = getMinWeightSum(eventId, otherEventId);
            double newSMin = currentSMin + weightDiff;
            putMinWeightSum(eventId, otherEventId, newSMin);
        }
    }

    private double calculateSMin(Long eventA, Long eventB) {
        Map<Long, Integer> weightsA = userEventWeights.get(eventA);
        Map<Long, Integer> weightsB = userEventWeights.get(eventB);

        if (weightsA == null || weightsB == null) {
            return 0.0;
        }

        double sMin = 0.0;
        for (Map.Entry<Long, Integer> entry : weightsA.entrySet()) {
            Long userId = entry.getKey();
            Integer weightA = entry.getValue();
            Integer weightB = weightsB.get(userId);

            if (weightB != null) {
                sMin += Math.min(weightA, weightB);
            }
        }

        return sMin;
    }

    private double calculateSimilarity(Long eventA, Long eventB) {
        double sMin = getMinWeightSum(eventA, eventB);

        if (sMin <= 0) {
            return 0.0;
        }

        double sA = totalWeights.getOrDefault(eventA, 0.0);
        double sB = totalWeights.getOrDefault(eventB, 0.0);

        if (sA <= 0 || sB <= 0) {
            return 0.0;
        }

        double denominator = Math.sqrt(sA) * Math.sqrt(sB);

        if (denominator == 0) {
            return 0.0;
        }

        return sMin / denominator;
    }

    // === РАБОТА С MIN_WEIGHTS_SUMS ===

    private void putMinWeightSum(Long eventA, Long eventB, double sum) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        minWeightsSums
                .computeIfAbsent(first, k -> new ConcurrentHashMap<>())
                .put(second, sum);

        log.debug("S_min сохранён: ({}, {}) = {}", first, second, sum);
    }

    private double getMinWeightSum(Long eventA, Long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        return minWeightsSums
                .computeIfAbsent(first, k -> new ConcurrentHashMap<>())
                .getOrDefault(second, 0.0);
    }

    // === ОТПРАВКА В KAFKA ===
    private void sendSimilarity(Long eventA, Long eventB, double score, Long timestamp) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        log.info("Сходство: ({}, {}) = {}, timestamp={}", first, second, score, timestamp);

        kafkaProducerService.sendSimilarity(first, second, score, timestamp);
    }
}