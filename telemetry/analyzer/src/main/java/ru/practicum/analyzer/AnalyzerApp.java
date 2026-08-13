package ru.practicum.analyzer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.annotation.EnableKafka;
import ru.practicum.analyzer.service.EventSimilarityProcessor;
import ru.practicum.analyzer.service.UserActionProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@EnableDiscoveryClient
@SpringBootApplication
@ConfigurationPropertiesScan
public class AnalyzerApp {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AnalyzerApp.class, args);

        final EventSimilarityProcessor eventSimilarityProcessor = context.getBean(EventSimilarityProcessor.class);
        final UserActionProcessor userActionProcessor = context.getBean(UserActionProcessor.class);

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        executorService.submit(eventSimilarityProcessor::start);
        executorService.submit(userActionProcessor::start);
    }
}