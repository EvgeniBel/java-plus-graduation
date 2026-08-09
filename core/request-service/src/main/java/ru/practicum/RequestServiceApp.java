package ru.practicum;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "ru.practicum")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "ru.practicum.client")
@Slf4j
public class RequestServiceApp {
    public static void main(String[] args) {
        log.info("Запуск RequestServiceApp");
        SpringApplication.run(RequestServiceApp.class, args);
        log.info("RequestServiceApp успешно запущен");
    }
}