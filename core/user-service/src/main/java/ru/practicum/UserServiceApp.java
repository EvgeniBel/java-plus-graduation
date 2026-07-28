package ru.practicum;


@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@Slf4j
public class UserServiceApp {
    public static void main(String[] args) {
        log.info("Запуск UserServiceApp");
        SpringApplication.run(UserServiceApp.class, args);
        log.info("UserServiceApp успешно запущен");
    }
}