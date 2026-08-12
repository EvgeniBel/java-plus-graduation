package ru.practicum.analyzer.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSimilarityConsumerService {

    private final EventSimilarityRepository eventSimilarityRepository;

    @KafkaListener(
            topics = "${kafka.topics.events-similarity}",
            containerFactory = "eventSimilarityKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeEventSimilarity(
            @Payload EventSimilarityAvro similarity,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.info("Получено сходство мероприятий: eventA={}, eventB={}, score={}, timestamp={}",
                    similarity.getEventA(), similarity.getEventB(), similarity.getScore(), similarity.getTimestamp());

            // Проверяем, существует ли уже запись
            var existingSimilarity = eventSimilarityRepository.findByEventAAndEventB(
                    similarity.getEventA(), similarity.getEventB());

            if (existingSimilarity.isPresent()) {
                // Обновляем существующую запись
                EventSimilarity eventSimilarity = existingSimilarity.get();
                eventSimilarity.setScore(similarity.getScore());
                eventSimilarity.setTimestamp(similarity.getTimestamp());
                eventSimilarity.setUpdatedAt(Instant.now());
                eventSimilarityRepository.save(eventSimilarity);
                log.info("Обновлено сходство мероприятий: eventA={}, eventB={}, score={}",
                        similarity.getEventA(), similarity.getEventB(), similarity.getScore());
            } else {
                // Создаем новую запись
                EventSimilarity eventSimilarity = EventSimilarity.builder()
                        .eventA(similarity.getEventA())
                        .eventB(similarity.getEventB())
                        .score(similarity.getScore())
                        .timestamp(similarity.getTimestamp())
                        .updatedAt(Instant.now())
                        .build();
                eventSimilarityRepository.save(eventSimilarity);
                log.info("Сохранено новое сходство мероприятий: eventA={}, eventB={}, score={}",
                        similarity.getEventA(), similarity.getEventB(), similarity.getScore());
            }

            acknowledgment.acknowledge();
            log.debug("Обработано сообщение: partition={}, offset={}", partition, offset);

        } catch (Exception e) {
            log.error("Ошибка обработки EventSimilarity: {}", e.getMessage(), e);
        }
    }
}