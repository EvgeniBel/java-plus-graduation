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
    private String topic;

    public void sendSimilarity(Long eventA, Long eventB, double score, long timestamp) {
        try {
            long first = Math.min(eventA, eventB);
            long second = Math.max(eventA, eventB);

            EventSimilarityAvro event = EventSimilarityAvro.newBuilder()
                    .setEventA(first)
                    .setEventB(second)
                    .setScore(score)
                    .setTimestamp(timestamp)
                    .build();

            kafkaTemplate.send(topic, first + "-" + second, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Ошибка отправки: {}", ex.getMessage(), ex);
                        }
                    });

            log.debug("Отправлено сходство: ({}, {}) = {}", first, second, score);
        } catch (Exception e) {
            log.error("Критическая ошибка: {}", e.getMessage(), e);
        }
    }
}