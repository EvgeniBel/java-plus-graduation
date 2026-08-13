package ru.practicum.analyzer.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.deserialize.EventSimilarityDeserializer;
import ru.practicum.analyzer.kafka.KafkaClient;
import ru.practicum.analyzer.mapper.EventSimilarityMapper;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityProcessor {

    private static final List<String> TOPICS = List.of("stats.events-similarity.v1");
    private static final String GROUP_ID = "similarity-analyzer-group";

    private final EventSimilarityRepository eventSimilarityRepository;
    private final KafkaClient<EventSimilarityAvro> kafkaConsumerConfig;

    private Consumer<String, EventSimilarityAvro> consumer;
    private volatile boolean running = true;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        this.consumer = kafkaConsumerConfig.getConsumer(GROUP_ID, EventSimilarityDeserializer.class);
        log.info("EventSimilarityProcessor initialized with group: {}", GROUP_ID);
    }

    @PreDestroy
    public void destroy() {
        running = false;
        if (consumer != null) {
            consumer.wakeup();
            log.info("EventSimilarityProcessor stopped");
        }
    }

    public void start() {
        try {
            consumer.subscribe(TOPICS);
            log.info("EventSimilarityProcessor started, subscribed to topics: {}", TOPICS);

            while (running) {
                ConsumerRecords<String, EventSimilarityAvro> records =
                        consumer.poll(Duration.ofSeconds(1));

                for (ConsumerRecord<String, EventSimilarityAvro> record : records) {
                    processRecord(record);
                }

                // Коммитим оффсеты после обработки
                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
        } catch (WakeupException e) {
            // Ожидаемое исключение при остановке
            log.info("EventSimilarityProcessor wakeup called");
        } catch (Exception e) {
            log.error("Error processing EventSimilarity messages", e);
        } finally {
            try {
                consumer.close();
                log.info("EventSimilarityProcessor consumer closed");
            } catch (Exception e) {
                log.error("Error closing consumer", e);
            }
        }
    }

    private void processRecord(ConsumerRecord<String, EventSimilarityAvro> record) {
        try {
            EventSimilarityAvro avro = record.value();
            if (avro == null) {
                log.warn("Received null EventSimilarityAvro at offset: {}", record.offset());
                return;
            }

            EventSimilarity eventSimilarity = EventSimilarityMapper.toEntity(avro);

            // Убеждаемся, что event1 < event2
            long eventA = Math.min(eventSimilarity.getEvent1(), eventSimilarity.getEvent2());
            long eventB = Math.max(eventSimilarity.getEvent1(), eventSimilarity.getEvent2());
            eventSimilarity.setEvent1(eventA);
            eventSimilarity.setEvent2(eventB);

            Optional<EventSimilarity> existing = eventSimilarityRepository
                    .findByEvent1AndEvent2(eventA, eventB);

            if (existing.isPresent()) {
                EventSimilarity existingSimilarity = existing.get();
                existingSimilarity.setSimilarity(eventSimilarity.getSimilarity());
                existingSimilarity.setTimestamp(eventSimilarity.getTimestamp());
                eventSimilarityRepository.save(existingSimilarity);

                log.debug("Updated similarity for events ({},{}): {}",
                        eventA, eventB, eventSimilarity.getSimilarity());
            } else {
                eventSimilarityRepository.save(eventSimilarity);
                log.debug("Saved new similarity for events ({},{}): {}",
                        eventA, eventB, eventSimilarity.getSimilarity());
            }
        } catch (Exception e) {
            log.error("Error processing EventSimilarity record at offset: {}", record.offset(), e);
        }
    }
}