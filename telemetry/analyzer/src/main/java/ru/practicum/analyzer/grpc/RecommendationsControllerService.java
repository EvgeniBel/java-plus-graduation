package ru.practicum.analyzer.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.analyzer.service.RecommendationService;
import ru.practicum.stats.service.dashboard.RecommendationsControllerGrpc;
import ru.practicum.stats.service.dashboard.RecommendationsProto;

import java.util.List;
import java.util.Map;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsControllerService extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(
            RecommendationsProto.UserPredictionsRequestProto request,
            StreamObserver<RecommendationsProto.RecommendedEventProto> responseObserver) {

        try {
            if (request == null) {
                log.error("Получен null запрос");
                responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                        .withDescription("Request cannot be null")
                        .asRuntimeException());
                return;
            }

            long userId = request.getUserId();
            int maxResults = request.getMaxResults() > 0 ? request.getMaxResults() : 10;

            log.info("Запрос рекомендаций для пользователя: userId={}, maxResults={}", userId, maxResults);

            List<Map.Entry<Long, Double>> recommendations =
                    recommendationService.getRecommendationsForUser(userId, maxResults);

            if (recommendations == null || recommendations.isEmpty()) {
                log.info("Нет рекомендаций для пользователя {}", userId);
                responseObserver.onCompleted();
                return;
            }

            for (Map.Entry<Long, Double> entry : recommendations) {
                RecommendationsProto.RecommendedEventProto response =
                        RecommendationsProto.RecommendedEventProto.newBuilder()
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
            RecommendationsProto.SimilarEventsRequestProto request,
            StreamObserver<RecommendationsProto.RecommendedEventProto> responseObserver) {

        try {
            if (request == null) {
                log.error("Получен null запрос");
                responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                        .withDescription("Request cannot be null")
                        .asRuntimeException());
                return;
            }

            long eventId = request.getEventId();
            long userId = request.getUserId();
            int maxResults = request.getMaxResults() > 0 ? request.getMaxResults() : 10;

            log.info("Запрос похожих мероприятий: eventId={}, userId={}, maxResults={}",
                    eventId, userId, maxResults);

            List<Map.Entry<Long, Double>> similarEvents =
                    recommendationService.getSimilarEvents(eventId, userId, maxResults);

            if (similarEvents == null || similarEvents.isEmpty()) {
                log.info("Нет похожих мероприятий для события {}", eventId);
                responseObserver.onCompleted();
                return;
            }

            for (Map.Entry<Long, Double> entry : similarEvents) {
                RecommendationsProto.RecommendedEventProto response =
                        RecommendationsProto.RecommendedEventProto.newBuilder()
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
            RecommendationsProto.InteractionsCountRequestProto request,
            StreamObserver<RecommendationsProto.RecommendedEventProto> responseObserver) {

        try {
            if (request == null) {
                log.error("Получен null запрос");
                responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                        .withDescription("Request cannot be null")
                        .asRuntimeException());
                return;
            }

            List<Long> eventIds = request.getEventIdList();

            log.info("Запрос количества взаимодействий для {} мероприятий",
                    eventIds != null ? eventIds.size() : 0);

            // Используем исправленный метод, который возвращает Double
            Map<Long, Double> interactions = recommendationService.getInteractionsCount(eventIds);

            if (interactions == null || interactions.isEmpty()) {
                log.info("Нет данных о взаимодействиях");
                responseObserver.onCompleted();
                return;
            }

            for (Map.Entry<Long, Double> entry : interactions.entrySet()) {
                RecommendationsProto.RecommendedEventProto response =
                        RecommendationsProto.RecommendedEventProto.newBuilder()
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