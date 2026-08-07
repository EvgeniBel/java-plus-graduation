package ru.practicum.aggregator.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;

    @Value("${kafka.topics.events-similarity}")
    private String similarityTopic;

    public void sendSimilarity(Long eventA, Long eventB, double score, long timestamp) {
        try {
            long first = Math.min(eventA, eventB);
            long second = Math.max(eventA, eventB);

            EventSimilarityAvro similarity = EventSimilarityAvro.newBuilder()
                    .setEventA(first)
                    .setEventB(second)
                    .setScore(score)
                    .setTimestamp(timestamp)
                    .build();

            String key = first + "-" + second;

            kafkaTemplate.send(similarityTopic, key, similarity)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Сходство отправлено: ({}, {}) = {}", first, second, score);
                        } else {
                            log.error("Ошибка отправки сходства: {}", ex.getMessage(), ex);
                        }
                    });

        } catch (Exception e) {
            log.error("Критическая ошибка при отправке сходства: {}", e.getMessage(), e);
        }
    }
}