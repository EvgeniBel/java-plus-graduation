package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.deserialize.UserActionDeserializer;
import ru.practicum.analyzer.kafka.KafkaClient;
import ru.practicum.analyzer.mapper.UserActionMapper;
import ru.practicum.analyzer.model.UserAction;
import ru.practicum.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.UserActionAvro;


import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionProcessor {

    private static final List<String> TOPICS = List.of("stats.user-actions.v1");
    private static final String GROUP_ID = "action-analyzer-group";

    private final UserActionRepository userActionRepository;
    private final KafkaClient<UserActionAvro> kafkaConsumerConfig;

    private Consumer<String, UserActionAvro> consumer;
    private volatile boolean running = true;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        this.consumer = kafkaConsumerConfig.getConsumer(GROUP_ID, UserActionDeserializer.class);
        log.info("UserActionProcessor initialized with group: {}", GROUP_ID);
    }

    @PreDestroy
    public void destroy() {
        running = false;
        if (consumer != null) {
            consumer.wakeup();
            log.info("UserActionProcessor stopped");
        }
    }

    public void start() {
        try {
            consumer.subscribe(TOPICS);
            log.info("UserActionProcessor started, subscribed to topics: {}", TOPICS);

            while (running) {
                ConsumerRecords<String, UserActionAvro> records =
                        consumer.poll(Duration.ofSeconds(1));

                for (ConsumerRecord<String, UserActionAvro> record : records) {
                    processRecord(record);
                }

                // Коммитим оффсеты после обработки
                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
        } catch (WakeupException e) {
            // Ожидаемое исключение при остановке
            log.info("UserActionProcessor wakeup called");
        } catch (Exception e) {
            log.error("Error processing UserAction messages", e);
        } finally {
            try {
                consumer.close();
                log.info("UserActionProcessor consumer closed");
            } catch (Exception e) {
                log.error("Error closing consumer", e);
            }
        }
    }

    private void processRecord(ConsumerRecord<String, UserActionAvro> record) {
        try {
            UserActionAvro avro = record.value();
            if (avro == null) {
                log.warn("Received null UserActionAvro at offset: {}", record.offset());
                return;
            }

            UserAction userAction = UserActionMapper.toEntity(avro);

            Optional<UserAction> existing = userActionRepository
                    .findByUserIdAndEventId(userAction.getUserId(), userAction.getEventId());

            if (existing.isPresent()) {
                UserAction existingAction = existing.get();
                // Оставляем максимальный рейтинг (вес действия)
                if (userAction.getRating() > existingAction.getRating()) {
                    existingAction.setRating(userAction.getRating());
                    existingAction.setTimestamp(userAction.getTimestamp());
                    userActionRepository.save(existingAction);

                    log.debug("Updated action for user={}, event={}, rating={} (was={})",
                            userAction.getUserId(),
                            userAction.getEventId(),
                            userAction.getRating(),
                            existingAction.getRating());
                } else {
                    log.trace("Skipped update for user={}, event={}, rating={} (current={})",
                            userAction.getUserId(),
                            userAction.getEventId(),
                            userAction.getRating(),
                            existingAction.getRating());
                }
            } else {
                userActionRepository.save(userAction);
                log.debug("Saved new action for user={}, event={}, rating={}",
                        userAction.getUserId(),
                        userAction.getEventId(),
                        userAction.getRating());
            }
        } catch (Exception e) {
            log.error("Error processing UserAction record at offset: {}", record.offset(), e);
        }
    }
}
