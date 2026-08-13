package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.model.UserAction;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.stats.service.dashboard.RecommendationsProto;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    /**
     * Получение рекомендаций для пользователя на основе его истории взаимодействий
     */
    public List<RecommendationsProto.RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        log.debug("Getting recommendations for user: {}, maxResults: {}", userId, maxResults);

        // Получаем все действия пользователя
        List<UserAction> userActions = userActionRepository.findAllByUserId(userId);

        if (userActions.isEmpty()) {
            log.debug("No actions found for user: {}", userId);
            return Collections.emptyList();
        }

        // Получаем ID событий, с которыми взаимодействовал пользователь
        Set<Long> interactedEventIds = userActions.stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        // Получаем уникальные ID событий для запроса в БД
        List<Long> eventIds = userActions.stream()
                .map(UserAction::getEventId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        Map<Long, List<EventSimilarity>> similaritiesByEvent = getSimilaritiesGroupedByEvent(eventIds);

        // Вычисляем веса
        Map<Long, Double> eventScores = new HashMap<>();
        Map<Long, Integer> eventCounts = new HashMap<>();

        for (UserAction action : userActions) {
            long eventId = action.getEventId();
            double rating = action.getRating().doubleValue();

            // Получаем похожие события из мапы
            List<EventSimilarity> similarities = similaritiesByEvent.getOrDefault(
                    eventId,
                    Collections.emptyList()
            );

            for (EventSimilarity similarity : similarities) {
                long similarEventId = similarity.getEvent1() == eventId
                        ? similarity.getEvent2()
                        : similarity.getEvent1();

                // Исключаем события, с которыми пользователь уже взаимодействовал
                if (interactedEventIds.contains(similarEventId)) {
                    continue;
                }

                // Вес = схожесть * рейтинг действия пользователя
                double weight = similarity.getSimilarity() * rating;

                eventScores.merge(similarEventId, weight, Double::sum);
                eventCounts.merge(similarEventId, 1, Integer::sum);
            }
        }

        // Нормализуем scores и создаем Proto объекты
        List<RecommendationsProto.RecommendedEventProto> recommendations = eventScores.entrySet().stream()
                .map(entry -> {
                    long eventId = entry.getKey();
                    double totalScore = entry.getValue();
                    int count = eventCounts.getOrDefault(eventId, 1);
                    double normalizedScore = totalScore / count;

                    return RecommendationsProto.RecommendedEventProto.newBuilder()
                            .setEventId(eventId)
                            .setScore((float) normalizedScore)
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(maxResults > 0 ? maxResults : 10)
                .collect(Collectors.toList());

        log.debug("Found {} recommendations for user: {}", recommendations.size(), userId);
        return recommendations;
    }

    /**
     * Получение похожих событий для заданного события с учетом пользователя
     */
    public List<RecommendationsProto.RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        log.debug("Getting similar events for event: {}, user: {}, maxResults: {}",
                eventId, userId, maxResults);

        // Получаем все похожие события для заданного eventId
        List<EventSimilarity> similarities = eventSimilarityRepository
                .findByEvent1OrEvent2OrderBySimilarityDesc(eventId);

        // Если пользователь указан, исключаем события, с которыми он уже взаимодействовал
        Set<Long> interactedEventIds = Collections.emptySet();
        if (userId > 0) {
            List<UserAction> userActions = userActionRepository.findAllByUserId(userId);
            interactedEventIds = userActions.stream()
                    .map(UserAction::getEventId)
                    .collect(Collectors.toSet());
        }

        final Set<Long> finalInteractedEventIds = interactedEventIds;

        List<RecommendationsProto.RecommendedEventProto> similarEvents = similarities.stream()
                .map(similarity -> {
                    long similarEventId = similarity.getEvent1() == eventId
                            ? similarity.getEvent2()
                            : similarity.getEvent1();

                    return RecommendationsProto.RecommendedEventProto.newBuilder()
                            .setEventId(similarEventId)
                            .setScore(similarity.getSimilarity())
                            .build();
                })
                .filter(event -> !finalInteractedEventIds.contains(event.getEventId()))
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(maxResults > 0 ? maxResults : 10)
                .collect(Collectors.toList());

        log.debug("Found {} similar events for event: {}", similarEvents.size(), eventId);
        return similarEvents;
    }

    /**
     * Получение количества взаимодействий с указанными событиями
     * Используется метод findAllByEventIdIn для массовой загрузки
     */
    public Map<Long, Double> getInteractionsCount(List<Long> eventIds) {
        log.debug("Getting interactions count for {} events", eventIds != null ? eventIds.size() : 0);

        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Получаем все действия для указанных событий одним запросом
        List<UserAction> allActions = userActionRepository.findAllByEventIdIn(eventIds);

        // Группируем действия по eventId
        Map<Long, List<UserAction>> actionsByEvent = allActions.stream()
                .collect(Collectors.groupingBy(UserAction::getEventId));

        // Для каждого eventId рассчитываем score
        Map<Long, Double> result = new HashMap<>();

        for (Long eventId : eventIds) {
            List<UserAction> actions = actionsByEvent.getOrDefault(eventId, Collections.emptyList());

            if (actions.isEmpty()) {
                result.put(eventId, 0.0);
                continue;
            }

            // Подсчет уникальных пользователей и суммарного рейтинга
            Set<Long> uniqueUsers = new HashSet<>();
            double totalRating = 0.0;

            for (UserAction action : actions) {
                uniqueUsers.add(action.getUserId());
                totalRating += action.getRating();
            }

            // Score = количество уникальных пользователей * средний рейтинг
            double avgRating = totalRating / actions.size();
            double score = uniqueUsers.size() * avgRating;

            result.put(eventId, score);

            log.debug("Event {}: {} unique users, avg rating: {}, score: {}",
                    eventId, uniqueUsers.size(), avgRating, score);
        }

        return result;
    }

    /**
     * Получение общего количества взаимодействий (без учета рейтинга)
     * Просто количество записей для каждого события
     */
    public Map<Long, Double> getInteractionsCountSimple(List<Long> eventIds) {
        log.debug("Getting simple interactions count for {} events", eventIds != null ? eventIds.size() : 0);

        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UserAction> allActions = userActionRepository.findAllByEventIdIn(eventIds);
        Map<Long, List<UserAction>> actionsByEvent = allActions.stream()
                .collect(Collectors.groupingBy(UserAction::getEventId));

        Map<Long, Double> result = new HashMap<>();
        for (Long eventId : eventIds) {
            List<UserAction> actions = actionsByEvent.getOrDefault(eventId, Collections.emptyList());
            result.put(eventId, (double) actions.size());
        }

        return result;
    }

    private Map<Long, List<EventSimilarity>> getSimilaritiesGroupedByEvent(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashMap<>();
        }

        List<EventSimilarity> allSimilarities = eventSimilarityRepository
                .findAllByEventIds(eventIds);

        // Группируем в Map через Stream
        return allSimilarities.stream()
                .flatMap(sim -> Stream.of(
                        Map.entry(sim.getEvent1(), sim),
                        Map.entry(sim.getEvent2(), sim)
                ))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        HashMap::new,
                        Collectors.mapping(
                                Map.Entry::getValue,
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> {
                                            list.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
                                            return list;
                                        }
                                )
                        )
                ));
    }

}