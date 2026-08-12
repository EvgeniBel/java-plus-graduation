package ru.practicum.analyzer.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.proto.*;

import java.util.List;
import java.util.Map;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsControllerService extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(
            UserPredictionsRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver) {

        try {
            long userId = request.getUserId();
            int maxResults = request.getMaxResults() > 0 ? request.getMaxResults() : 10;

            log.info("Запрос рекомендаций для пользователя: userId={}, maxResults={}", userId, maxResults);

            List<Map.Entry<Long, Double>> recommendations =
                    recommendationService.getRecommendationsForUser(userId, maxResults);

            for (Map.Entry<Long, Double> entry : recommendations) {
                RecommendedEventProto response = RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue().floatValue())
                        .build();
                responseObserver.onNext(response);
            }

            responseObserver.onCompleted();
            log.info("Отправлено {} рекомендаций для пользователя {}", recommendations.size(), userId);

        } catch (Exception e) {
            log.error("Ошибка получения рекомендаций для пользователя: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(String.format("Ошибка получения рекомендаций: %s", e.getMessage()))
                    .asRuntimeException());
        }
    }

    @Override
    public void getSimilarEvents(
            SimilarEventsRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver) {

        try {
            long eventId = request.getEventId();
            long userId = request.getUserId();
            int maxResults = request.getMaxResults() > 0 ? request.getMaxResults() : 10;

            log.info("Запрос похожих мероприятий: eventId={}, userId={}, maxResults={}",
                    eventId, userId, maxResults);

            List<Map.Entry<Long, Double>> similarEvents =
                    recommendationService.getSimilarEvents(eventId, userId, maxResults);

            for (Map.Entry<Long, Double> entry : similarEvents) {
                RecommendedEventProto response = RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue().floatValue())
                        .build();
                responseObserver.onNext(response);
            }

            responseObserver.onCompleted();
            log.info("Отправлено {} похожих мероприятий для события {}", similarEvents.size(), eventId);

        } catch (Exception e) {
            log.error("Ошибка получения похожих мероприятий: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(String.format("Ошибка получения похожих мероприятий: %s", e.getMessage()))
                    .asRuntimeException());
        }
    }

    @Override
    public void getInteractionsCount(
            InteractionsCountRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver) {

        try {
            List<Long> eventIds = request.getEventIdsList();

            log.info("Запрос количества взаимодействий для {} мероприятий", eventIds.size());

            Map<Long, Long> interactions = recommendationService.getInteractionsCount(eventIds);

            for (Map.Entry<Long, Long> entry : interactions.entrySet()) {
                RecommendedEventProto response = RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue().floatValue())
                        .build();
                responseObserver.onNext(response);
            }

            responseObserver.onCompleted();
            log.info("Отправлены количества взаимодействий для {} мероприятий", interactions.size());

        } catch (Exception e) {
            log.error("Ошибка получения количества взаимодействий: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(String.format("Ошибка получения количества взаимодействий: %s", e.getMessage()))
                    .asRuntimeException());
        }
    }
}