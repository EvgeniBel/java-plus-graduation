package ru.practicum.grpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.telemetry.messages.*;
import ru.practicum.telemetry.services.AnalyzerControllerGrpc;

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
    private AnalyzerControllerGrpc.AnalyzerControllerBlockingStub analyzerStub;


    public List<RecommendedEvent> getRecommendationsForUser(Long userId, int maxResults) {
        try {
            log.info("Запрос рекомендаций: userId={}, maxResults={}", userId, maxResults);

            UserPredictionsRequest request = UserPredictionsRequest.newBuilder()
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            Iterator<RecommendedEvent> iterator = analyzerStub.getRecommendationsForUser(request);
            List<RecommendedEvent> result = asStream(iterator).collect(Collectors.toList());

            log.info("Получено {} рекомендаций", result.size());
            return result;

        } catch (Exception e) {
            log.error("Ошибка получения рекомендаций: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<RecommendedEvent> getSimilarEvents(Long eventId, Long userId, int maxResults) {
        try {
            log.info("Запрос похожих событий: eventId={}, userId={}", eventId, userId);

            SimilarEventsRequest request = SimilarEventsRequest.newBuilder()
                    .setEventId(eventId)
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            Iterator<RecommendedEvent> iterator = analyzerStub.getSimilarEvents(request);
            List<RecommendedEvent> result = asStream(iterator).collect(Collectors.toList());

            log.info("Получено {} похожих событий", result.size());
            return result;

        } catch (Exception e) {
            log.error("Ошибка получения похожих событий: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<RecommendedEvent> getInteractionsCount(List<Long> eventIds) {
        try {
            log.info("Запрос взаимодействий для {} событий", eventIds.size());

            InteractionsCountRequest request = InteractionsCountRequest.newBuilder()
                    .addAllEventIds(eventIds)
                    .build();

            Iterator<RecommendedEvent> iterator = analyzerStub.getInteractionsCount(request);
            List<RecommendedEvent> result = asStream(iterator).collect(Collectors.toList());

            log.info("Получены взаимодействия для {} событий", result.size());
            return result;

        } catch (Exception e) {
            log.error("Ошибка получения взаимодействий: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private Stream<RecommendedEvent> asStream(Iterator<RecommendedEvent> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}