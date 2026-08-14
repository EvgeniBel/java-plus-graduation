package ru.practicum.aggregator.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;

    @Value("${kafka.topics.events-similarity}")
    private String topic;

    public void sendSimilarity(Long eventA, Long eventB, double score, long timestamp) {
        if (eventA == null || eventB == null || score < 0 || timestamp <= 0) {
            log.warn("Невалидные данные для отправки: a={}, b={}, score={}, time={}",
                    eventA, eventB, score, timestamp);
            return;
        }

        try {
            long first = Math.min(eventA, eventB);
            long second = Math.max(eventA, eventB);
            String key = first + "-" + second;

            EventSimilarityAvro event = EventSimilarityAvro.newBuilder()
                    .setEventA(first)
                    .setEventB(second)
                    .setScore(score)
                    .setTimestamp(timestamp)
                    .build();

            CompletableFuture<SendResult<String, EventSimilarityAvro>> future =
                    kafkaTemplate.send(topic, key, event);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Ошибка отправки сходства для событий ({}, {})", first, second, ex);
                } else {
                    log.debug("Отправлено сходство: ({}, {}) = {}, оффсет: {}",
                            first, second, score, result.getRecordMetadata().offset());
                }
            });

        } catch (Exception e) {
            log.error("Критическая ошибка при отправке: {}", e.getMessage(), e);
        }
    }
}