package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaTopicInitializer {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @PostConstruct
    public void initializeTopics() {
        log.info("Инициализация Kafka топиков...");
        createTopicsIfNotExist();
    }

    private void createTopicsIfNotExist() {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);

        try (AdminClient adminClient = AdminClient.create(props)) {
            // Топик для действий пользователей
            NewTopic userActionsTopic = new NewTopic(
                    "stats.user-actions.v1",
                    3,  // partitions
                    (short) 1  // replication-factor
            );

            // Топик для сходства мероприятий
            NewTopic eventsSimilarityTopic = new NewTopic(
                    "stats.events-similarity.v1",
                    3,  // partitions
                    (short) 1  // replication-factor
            );

            log.info("Создание топиков (если не существуют)...");

            adminClient.createTopics(Arrays.asList(userActionsTopic, eventsSimilarityTopic))
                    .all()
                    .get();

            log.info("✅ Топики успешно созданы или уже существуют");

        } catch (ExecutionException e) {
            if (e.getCause() instanceof org.apache.kafka.common.errors.TopicExistsException) {
                log.info("✅ Топики уже существуют, пропускаем создание");
            } else {
                log.error("❌ Ошибка при создании топиков: {}", e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("❌ Ошибка при создании топиков: {}", e.getMessage(), e);
        }
    }
}
