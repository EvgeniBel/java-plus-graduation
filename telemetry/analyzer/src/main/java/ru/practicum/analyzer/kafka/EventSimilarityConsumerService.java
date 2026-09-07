package ru.practicum.analyzer.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.avro.deserializer.EventSimilarityDeserializer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSimilarityConsumerService {

    private final EventSimilarityRepository repository;
    private final EventSimilarityDeserializer deserializer = new EventSimilarityDeserializer();

    @KafkaListener(
            topics = "${kafka.topics.events-similarity}",
            containerFactory = "eventSimilarityKafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(String key, byte[] data) {
        try {
            // Десериализуем Avro из байтов
            EventSimilarityAvro similarity = deserializer.deserialize("stats.events-similarity.v1", data);

            log.info("Получено сходство: key={}, eventA={}, eventB={}, score={}",
                    key, similarity.getEventA(), similarity.getEventB(), similarity.getScore());

            EventSimilarity entity = repository
                    .findByEventAAndEventB(similarity.getEventA(), similarity.getEventB())
                    .map(existing -> {
                        existing.setScore(similarity.getScore());
                        existing.setTimestamp(similarity.getTimestamp());
                        log.info("Обновлено: eventA={}, eventB={}, score={}",
                                similarity.getEventA(), similarity.getEventB(), similarity.getScore());
                        return existing;
                    })
                    .orElseGet(() -> EventSimilarity.builder()
                            .eventA(similarity.getEventA())
                            .eventB(similarity.getEventB())
                            .score(similarity.getScore())
                            .timestamp(similarity.getTimestamp())
                            .build());

            repository.save(entity);

        } catch (Exception e) {
            log.error("Ошибка обработки: {}", e.getMessage(), e);
        }
    }
}