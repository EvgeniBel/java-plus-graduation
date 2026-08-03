package ru.practicum;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(scanBasePackages = "ru.practicum")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "ru.practicum.client")
@EnableRetry
@Slf4j
public class EventServiceApp {
    public static void main(String[] args) {
        log.info("Запуск EventServiceApplication");
        SpringApplication.run(EventServiceApp.class, args);
        log.info("EventServiceApplication успешно запущен");
    }
}