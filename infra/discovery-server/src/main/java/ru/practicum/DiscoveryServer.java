package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
@Slf4j
public class DiscoveryServer {
    public static void main(String[] args) {
        log.info("Запуск DiscoveryServer приложения");
        SpringApplication.run(DiscoveryServer.class, args);
        log.info("DiscoveryServer успешно запущен");
    }
}