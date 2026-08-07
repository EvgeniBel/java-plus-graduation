package ru.practicum.analyzer.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.analyzer.service.RecommendationService;
import ru.practicum.telemetry.messages.*;
import ru.practicum.telemetry.services.AnalyzerControllerGrpc;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AnalyzerGrpcService extends AnalyzerControllerGrpc.AnalyzerControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(
            UserPredictionsRequest request,
            StreamObserver<RecommendedEvent> responseObserver) {

        Long userId = request.getUserId();
        int maxResults = request.getMaxResults();

        log.info("📊 Запрос рекомендаций: userId={}, maxResults={}", userId, maxResults);

        try {
            List<RecommendationService.RecommendedEvent> recommendations =
                    recommendationService.getRecommendationsForUser(userId, maxResults);

            for (RecommendationService.RecommendedEvent event : recommendations) {
                RecommendedEvent proto = RecommendedEvent.newBuilder()
                        .setEventId(event.getEventId())
                        .setScore(event.getScore())
                        .build();
                responseObserver.onNext(proto);
            }

            log.info("Отправлено {} рекомендаций для пользователя {}", recommendations.size(), userId);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Ошибка получения рекомендаций: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getSimilarEvents(
            SimilarEventsRequest request,
            StreamObserver<RecommendedEvent> responseObserver) {

        Long eventId = request.getEventId();
        Long userId = request.getUserId();
        int maxResults = request.getMaxResults();

        log.info("Запрос похожих мероприятий: eventId={}, userId={}, maxResults={}",
                eventId, userId, maxResults);

        try {
            List<RecommendationService.RecommendedEvent> similarEvents =
                    recommendationService.getSimilarEvents(eventId, userId, maxResults);

            for (RecommendationService.RecommendedEvent event : similarEvents) {
                RecommendedEvent proto = RecommendedEvent.newBuilder()
                        .setEventId(event.getEventId())
                        .setScore(event.getScore())
                        .build();
                responseObserver.onNext(proto);
            }

            log.info("Отправлено {} похожих мероприятий", similarEvents.size());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Ошибка получения похожих мероприятий: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getInteractionsCount(
            InteractionsCountRequest request,
            StreamObserver<RecommendedEvent> responseObserver) {

        List<Long> eventIds = request.getEventIdsList();

        log.info("Запрос суммы весов для {} мероприятий", eventIds.size());

        try {
            List<RecommendationService.RecommendedEvent> weights =
                    recommendationService.getInteractionsCount(eventIds);

            for (RecommendationService.RecommendedEvent event : weights) {
                RecommendedEvent proto = RecommendedEvent.newBuilder()
                        .setEventId(event.getEventId())
                        .setScore(event.getScore())
                        .build();
                responseObserver.onNext(proto);
            }

            log.info("Отправлены веса для {} мероприятий", weights.size());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Ошибка получения суммы весов: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }
}