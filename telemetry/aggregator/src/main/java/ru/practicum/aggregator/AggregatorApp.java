package ru.practicum.aggregator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.annotation.EnableKafka;

@Slf4j
@EnableKafka
@EnableDiscoveryClient
@SpringBootApplication
public class AggregatorApp {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AggregatorApp.class, args);

    }
}