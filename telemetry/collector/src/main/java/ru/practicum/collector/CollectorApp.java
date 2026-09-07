package ru.practicum.collector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;

@Slf4j
@EnableKafka
@EnableDiscoveryClient
@SpringBootApplication
public class CollectorApp {

    public static void main(String[] args) {
        log.info("Запуск Collector Service");
        SpringApplication.run(CollectorApp.class, args);
        log.info("Collector Service успешно запущен");
    }
}