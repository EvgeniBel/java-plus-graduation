package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import ru.practicum.aggregator.mapper.RecommendedEventMapper;
import ru.practicum.dto.event.RecommendedEventDto;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.grpc.AnalyzerGrpcClient;
import ru.practicum.grpc.CollectorGrpcClient;

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

    public void sendViewAction(Long userId, Long eventId) {
        try {
            log.info("Отправка просмотра: userId={}, eventId={}", userId, eventId);
            boolean success = collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
            if (success) {
                log.info("Просмотр отправлен: userId={}, eventId={}", userId, eventId);
            } else {
                log.warn("Не удалось отправить просмотр: userId={}, eventId={}", userId, eventId);
            }
        } catch (Exception e) {
            log.error("Ошибка отправки просмотра: userId={}, eventId={}", userId, eventId, e);
        }
    }

    public void sendLikeAction(Long userId, Long eventId) {
        try {
            log.info("Отправка лайка: userId={}, eventId={}", userId, eventId);
            boolean success = collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
            if (success) {
                log.info("Лайк отправлен: userId={}, eventId={}", userId, eventId);
            } else {
                log.warn("Не удалось отправить лайк: userId={}, eventId={}", userId, eventId);
            }
        } catch (Exception e) {
            log.error("Ошибка отправки лайка: userId={}, eventId={}", userId, eventId, e);
        }
    }

    public void sendRegisterAction(Long userId, Long eventId) {
        try {
            log.info("Отправка регистрации: userId={}, eventId={}", userId, eventId);
            boolean success = collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
            if (success) {
                log.info("Регистрация отправлена: userId={}, eventId={}", userId, eventId);
            } else {
                log.warn("Не удалось отправить регистрацию: userId={}, eventId={}", userId, eventId);
            }
        } catch (Exception e) {
            log.error("Ошибка отправки регистрации: userId={}, eventId={}", userId, eventId, e);
        }
    }

    public double getEventRating(Long eventId) {
        try {
            List<RecommendedEventProto> result = analyzerClient.getInteractionsCount(List.of(eventId));
            if (result != null && !result.isEmpty()) {
                double rating = result.getFirst().getScore();
                log.debug("Рейтинг события {}: {}", eventId, rating);
                return rating;
            }
            return 0.0;
        } catch (Exception e) {
            log.warn("Не удалось получить рейтинг для события {}: {}", eventId, e.getMessage());
            return 0.0;
        }
    }

    public Map<Long, Double> getEventsRatings(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }

        try {
            List<RecommendedEventProto> results = analyzerClient.getInteractionsCount(eventIds);
            return results.stream()
                    .collect(Collectors.toMap(
                            RecommendedEventProto::getEventId,
                            RecommendedEventProto::getScore
                    ));
        } catch (Exception e) {
            log.warn("Не удалось получить рейтинги для событий: {}", e.getMessage());
            return Map.of();
        }
    }

    public List<RecommendedEventDto> getRecommendationsForUser(Long userId, int maxResults) {
        try {
            log.info("Запрос рекомендаций: userId={}, maxResults={}", userId, maxResults);
            List<RecommendedEventProto> events = analyzerClient.getRecommendationsForUser(userId, maxResults);
            return mapper.toDtoList(events);
        } catch (Exception e) {
            log.error("Ошибка получения рекомендаций: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<RecommendedEventDto> getSimilarEvents(Long eventId, Long userId, int maxResults) {
        try {
            log.info("Запрос похожих событий: eventId={}, userId={}", eventId, userId);
            List<RecommendedEventProto> events = analyzerClient.getSimilarEvents(eventId, userId, maxResults);
            return mapper.toDtoList(events);
        } catch (Exception e) {
            log.error("Ошибка получения похожих событий: {}", e.getMessage(), e);
            return List.of();
        }
    }
}
