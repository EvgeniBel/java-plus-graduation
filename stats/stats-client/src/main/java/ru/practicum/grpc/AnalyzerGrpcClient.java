package ru.practicum.grpc;

import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.stats.service.dashboard.RecommendationsControllerGrpc;
import ru.practicum.stats.service.dashboard.RecommendationsProto.InteractionsCountRequestProto;
import ru.practicum.stats.service.dashboard.RecommendationsProto.RecommendedEventProto;
import ru.practicum.stats.service.dashboard.RecommendationsProto.SimilarEventsRequestProto;
import ru.practicum.stats.service.dashboard.RecommendationsProto.UserPredictionsRequestProto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerGrpcClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerStub;

    public List<RecommendedEventProto> getRecommendationsForUser(Long userId, int maxResults) {
        try {
            if (userId == null || userId <= 0) {
                log.warn("Невалидный userId: {}", userId);
                return new ArrayList<>();
            }

            log.info("Запрос рекомендаций: userId={}, maxResults={}", userId, maxResults);

            UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                    .setUserId(userId)
                    .setMaxResults(maxResults > 0 ? maxResults : 10)
                    .build();

            Iterator<RecommendedEventProto> iterator = analyzerStub.getRecommendationsForUser(request);
            List<RecommendedEventProto> result = asStream(iterator).collect(Collectors.toList());

            log.info("Получено {} рекомендаций для пользователя {}", result.size(), userId);
            return result;

        } catch (StatusRuntimeException e) {
            log.error("Ошибка gRPC вызова Analyzer: status={}, message={}",
                    e.getStatus().getCode(), e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Ошибка получения рекомендаций: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<RecommendedEventProto> getSimilarEvents(Long eventId, Long userId, int maxResults) {
        try {
            if (eventId == null || eventId <= 0) {
                log.warn("Невалидный eventId: {}", eventId);
                return new ArrayList<>();
            }

            log.info("Запрос похожих событий: eventId={}, userId={}, maxResults={}",
                    eventId, userId, maxResults);

            SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                    .setEventId(eventId)
                    .setUserId(userId != null ? userId : 0)
                    .setMaxResults(maxResults > 0 ? maxResults : 10)
                    .build();

            Iterator<RecommendedEventProto> iterator = analyzerStub.getSimilarEvents(request);
            List<RecommendedEventProto> result = asStream(iterator).collect(Collectors.toList());

            log.info("Получено {} похожих событий для eventId={}", result.size(), eventId);
            return result;

        } catch (StatusRuntimeException e) {
            log.error("Ошибка gRPC вызова Analyzer: status={}, message={}",
                    e.getStatus().getCode(), e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Ошибка получения похожих событий: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            log.warn("Список eventIds пуст или null");
            return new ArrayList<>();
        }

        try {
            log.info("Запрос взаимодействий для {} событий", eventIds.size());

            InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                    .addAllEventId(eventIds)
                    .build();

            Iterator<RecommendedEventProto> iterator = analyzerStub.getInteractionsCount(request);
            List<RecommendedEventProto> result = asStream(iterator).collect(Collectors.toList());

            log.info("Получены взаимодействия для {} событий", result.size());
            return result;

        } catch (StatusRuntimeException e) {
            log.error("Ошибка gRPC вызова Analyzer: status={}, message={}",
                    e.getStatus().getCode(), e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Ошибка получения взаимодействий: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}