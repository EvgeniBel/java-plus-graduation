package ru.practicum;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties
@Slf4j
public class GatewayServer {
    public static void main(String[] args) {
        log.info("Запуск Gateway сервера");
        SpringApplication.run(GatewayServer.class, args);
        log.info("Gateway успешно запущен");
    }
}