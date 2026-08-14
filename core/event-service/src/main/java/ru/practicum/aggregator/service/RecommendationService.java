package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.aggregator.mapper.RecommendedEventMapper;
import ru.practicum.dto.event.RecommendedEventDto;
import ru.practicum.grpc.AnalyzerGrpcClient;
import ru.practicum.grpc.CollectorGrpcClient;
import ru.practicum.stats.service.collector.UserActionOuterClass;
import ru.practicum.stats.service.dashboard.RecommendationsProto;

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
        if (userId == null || userId <= 0 || eventId == null || eventId <= 0) {
            log.warn("Невалидные данные для отправки просмотра: userId={}, eventId={}", userId, eventId);
            return;
        }

        try {
            log.debug("Отправка просмотра: userId={}, eventId={}", userId, eventId);
            boolean success = collectorClient.sendUserAction(userId, eventId,
                    UserActionOuterClass.ActionTypeProto.ACTION_VIEW);
            if (success) {
                log.debug("Просмотр отправлен: userId={}, eventId={}", userId, eventId);
            } else {
                log.warn("Не удалось отправить просмотр: userId={}, eventId={}", userId, eventId);
            }
        } catch (Exception e) {
            log.error("Ошибка отправки просмотра: userId={}, eventId={}", userId, eventId, e);
        }
    }

    public void sendLikeAction(Long userId, Long eventId) {
        if (userId == null || userId <= 0 || eventId == null || eventId <= 0) {
            log.warn("Невалидные данные для отправки лайка: userId={}, eventId={}", userId, eventId);
            return;
        }

        try {
            log.debug("Отправка лайка: userId={}, eventId={}", userId, eventId);
            boolean success = collectorClient.sendUserAction(userId, eventId,
                    UserActionOuterClass.ActionTypeProto.ACTION_LIKE);
            if (success) {
                log.debug("Лайк отправлен: userId={}, eventId={}", userId, eventId);
            } else {
                log.warn("Не удалось отправить лайк: userId={}, eventId={}", userId, eventId);
            }
        } catch (Exception e) {
            log.error("Ошибка отправки лайка: userId={}, eventId={}", userId, eventId, e);
        }
    }

    public void sendRegisterAction(Long userId, Long eventId) {
        if (userId == null || userId <= 0 || eventId == null || eventId <= 0) {
            log.warn("Невалидные данные для отправки регистрации: userId={}, eventId={}", userId, eventId);
            return;
        }

        try {
            log.debug("Отправка регистрации: userId={}, eventId={}", userId, eventId);
            boolean success = collectorClient.sendUserAction(userId, eventId,
                    UserActionOuterClass.ActionTypeProto.ACTION_REGISTER);
            if (success) {
                log.debug("Регистрация отправлена: userId={}, eventId={}", userId, eventId);
            } else {
                log.warn("Не удалось отправить регистрацию: userId={}, eventId={}", userId, eventId);
            }
        } catch (Exception e) {
            log.error("Ошибка отправки регистрации: userId={}, eventId={}", userId, eventId, e);
        }
    }

    public double getEventRating(Long eventId) {
        if (eventId == null || eventId <= 0) {
            log.warn("Невалидный eventId для получения рейтинга: {}", eventId);
            return 0.0;
        }

        try {
            // Используем исправленный метод, который возвращает Double
            List<RecommendationsProto.RecommendedEventProto> result = analyzerClient.getInteractionsCount(List.of(eventId));
            if (result != null && !result.isEmpty()) {
                double rating = result.get(0).getScore();
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
            log.debug("Пустой список eventIds для получения рейтингов");
            return Map.of();
        }

        try {
            List<RecommendationsProto.RecommendedEventProto> results = analyzerClient.getInteractionsCount(eventIds);
            if (results == null || results.isEmpty()) {
                return Map.of();
            }
            return results.stream()
                    .collect(Collectors.toMap(
                            RecommendationsProto.RecommendedEventProto::getEventId,
                            RecommendationsProto.RecommendedEventProto::getScore,
                            (a, b) -> a  // на случай дубликатов
                    ));
        } catch (Exception e) {
            log.warn("Не удалось получить рейтинги для событий: {}", e.getMessage());
            return Map.of();
        }
    }

    public List<RecommendedEventDto> getRecommendationsForUser(Long userId, int maxResults) {
        if (userId == null || userId <= 0) {
            log.warn("Невалидный userId для получения рекомендаций: {}", userId);
            return List.of();
        }

        try {
            log.info("Запрос рекомендаций: userId={}, maxResults={}", userId, maxResults);
            List<RecommendationsProto.RecommendedEventProto> events =
                    analyzerClient.getRecommendationsForUser(userId, maxResults);
            return mapper.toDtoList(events);
        } catch (Exception e) {
            log.error("Ошибка получения рекомендаций: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<RecommendedEventDto> getSimilarEvents(Long eventId, Long userId, int maxResults) {
        if (eventId == null || eventId <= 0) {
            log.warn("Невалидный eventId для получения похожих событий: {}", eventId);
            return List.of();
        }

        try {
            log.info("Запрос похожих событий: eventId={}, userId={}", eventId, userId);
            List<RecommendationsProto.RecommendedEventProto> events =
                    analyzerClient.getSimilarEvents(eventId, userId, maxResults);
            return mapper.toDtoList(events);
        } catch (Exception e) {
            log.error("Ошибка получения похожих событий: {}", e.getMessage(), e);
            return List.of();
        }
    }
}
