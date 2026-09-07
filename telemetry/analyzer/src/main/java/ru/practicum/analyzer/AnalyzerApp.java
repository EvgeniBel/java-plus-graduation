package ru.practicum.analyzer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;

@Slf4j
@EnableKafka
@EnableDiscoveryClient
@EnableJpaAuditing
@SpringBootApplication
public class AnalyzerApp {

    public static void main(String[] args) {
        log.info("Запуск Analyzer Service");
        SpringApplication.run(AnalyzerApp.class, args);
        log.info("Analyzer Service успешно запущен");
    }
}