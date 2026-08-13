package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.practicum.aggregator.mapper.RecommendedEventMapper;
import ru.practicum.dto.event.RecommendedEventDto;
import ru.practicum.grpc.AnalyzerGrpcClient;
import ru.practicum.grpc.CollectorGrpcClient;
import ru.practicum.grpc.RecommendedEvent;
import ru.practicum.stats.service.collector.UserActionOuterClass;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final CollectorGrpcClient collectorClient;
    private final AnalyzerGrpcClient analyzerClient;
    private final RecommendedEventMapper mapper;

    // ===== ОТПРАВКА ДЕЙСТВИЙ =====

    public void sendAction(Long userId, Long eventId, UserActionOuterClass.ActionTypeProto type) {
        try {
            log.debug("Отправка действия: userId={}, eventId={}, type={}", userId, eventId, type);
            collectorClient.collectUserAction(userId, eventId, type);  // ← Исправлено
        } catch (Exception e) {
            log.warn("Не удалось отправить {}: {}", type, e.getMessage());
        }
    }

    public void sendViewAction(Long userId, Long eventId) {
        sendAction(userId, eventId, UserActionOuterClass.ActionTypeProto.ACTION_VIEW);
    }

    public void sendLikeAction(Long userId, Long eventId) {
        sendAction(userId, eventId, UserActionOuterClass.ActionTypeProto.ACTION_LIKE);
    }

    public void sendRegisterAction(Long userId, Long eventId) {
        sendAction(userId, eventId, UserActionOuterClass.ActionTypeProto.ACTION_REGISTER);
    }


    // ===== ПОЛУЧЕНИЕ РЕЙТИНГОВ =====

    @Cacheable(value = "eventRating", key = "#eventId")
    public double getEventRating(Long eventId) {
        try {
            return analyzerClient.getInteractionsCount(List.of(eventId))
                    .stream()
                    .findFirst()
                    .map(RecommendedEvent::getScore)  // ← Исправлено
                    .orElse(0.0);
        } catch (Exception e) {
            log.warn("Рейтинг события {}: {}", eventId, e.getMessage());
            return 0.0;
        }
    }

    @Cacheable(value = "eventsRatings", key = "#eventIds")
    public Map<Long, Double> getEventsRatings(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            return analyzerClient.getInteractionsCount(eventIds)
                    .stream()
                    .collect(Collectors.toMap(
                            RecommendedEvent::getEventId,  // ← Исправлено
                            RecommendedEvent::getScore     // ← Исправлено
                    ));
        } catch (Exception e) {
            log.warn("Рейтинги событий: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    // ===== РЕКОМЕНДАЦИИ =====

    public List<RecommendedEventDto> getRecommendationsForUser(Long userId, int maxResults) {
        try {
            return analyzerClient.getRecommendationsForUser(userId, maxResults)
                    .stream()
                    .map(mapper::toDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка получения рекомендаций: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<RecommendedEventDto> getSimilarEvents(Long eventId, Long userId, int maxResults) {
        try {
            return analyzerClient.getSimilarEvents(eventId, userId, maxResults)
                    .stream()
                    .map(mapper::toDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка получения похожих событий: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
