package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.aggregator.kafka.KafkaProducerService;
import ru.practicum.aggregator.model.UserActionEvent;
import ru.practicum.aggregator.util.ActionTypeUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregatorService {

    private final KafkaProducerService producer;

    // userId -> (eventId -> weight)
    private final Map<Long, Map<Long, Integer>> userWeights = new ConcurrentHashMap<>();

    // eventId -> totalWeight
    private final Map<Long, Double> eventTotal = new ConcurrentHashMap<>();

    // eventA -> (eventB -> minWeightSum)
    private final Map<Long, Map<Long, Double>> minSums = new ConcurrentHashMap<>();

    private static final double EPS = 1e-9;

    public synchronized void process(UserActionEvent action) {
        Long userId = action.getUserId();
        Long eventId = action.getEventId();
        int newWeight = ActionTypeUtils.getWeight(action.getActionType());
        int oldWeight = getWeight(userId, eventId);

        if (newWeight <= oldWeight) {
            log.debug("Вес не изменился: user={}, event={}", userId, eventId);
            return;
        }

        // Обновляем вес
        userWeights.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(eventId, newWeight);
        eventTotal.merge(eventId, (double) (newWeight - oldWeight), Double::sum);

        // Пересчитываем сходство
        recalculate(eventId, userId, oldWeight, newWeight, action.getTimestamp());
    }

    private int getWeight(Long userId, Long eventId) {
        Map<Long, Integer> weights = userWeights.get(userId);
        return weights == null ? 0 : weights.getOrDefault(eventId, 0);
    }

    private void recalculate(Long eventId, Long userId, int oldWeight, int newWeight, Long timestamp) {
        Map<Long, Integer> userEvents = userWeights.get(userId);
        if (userEvents == null) return;

        for (Map.Entry<Long, Integer> entry : userEvents.entrySet()) {
            Long otherId = entry.getKey();
            if (otherId.equals(eventId)) continue;

            int otherWeight = entry.getValue();
            updateMinSum(eventId, otherId, oldWeight, newWeight, otherWeight);

            double similarity = calcSimilarity(eventId, otherId);
            if (similarity > EPS) {
                producer.sendSimilarity(eventId, otherId, similarity, timestamp);
            }
        }
    }

    private void updateMinSum(Long a, Long b, int oldW, int newW, int otherW) {
        long first = Math.min(a, b);
        long second = Math.max(a, b);

        double delta = Math.min(newW, otherW) - Math.min(oldW, otherW);
        if (Math.abs(delta) < EPS) return;

        minSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>())
                .merge(second, delta, Double::sum);
    }

    private double calcSimilarity(Long a, Long b) {
        long first = Math.min(a, b);
        long second = Math.max(a, b);

        double sMin = minSums.getOrDefault(first, Map.of()).getOrDefault(second, 0.0);
        if (sMin <= EPS) return 0.0;

        double sA = eventTotal.getOrDefault(a, 0.0);
        double sB = eventTotal.getOrDefault(b, 0.0);
        if (sA <= EPS || sB <= EPS) return 0.0;

        return sMin / (Math.sqrt(sA) * Math.sqrt(sB));
    }
}