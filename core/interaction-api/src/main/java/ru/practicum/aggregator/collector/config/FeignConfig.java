package ru.practicum.aggregator.collector.config;

import feign.*;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

import static ru.practicum.constants.ApiConstants.USER_ID_HEADER;

@Slf4j
@Configuration
@ConditionalOnClass(FeignClient.class)
public class FeignConfig {

    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(
                100,
                TimeUnit.SECONDS.toMillis(1),
                3
        );
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor userHeaderInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String userId = request.getHeader(USER_ID_HEADER);
                if (userId != null) {
                    requestTemplate.header(USER_ID_HEADER, userId);
                }
            }
        };
    }

    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(
                10, TimeUnit.SECONDS,
                30, TimeUnit.SECONDS,
                true
        );
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }

    @Bean
    public Feign.Builder feignBuilder() {
        return Feign.builder()
                .retryer(feignRetryer())
                .options(feignOptions())
                .logger(new feign.slf4j.Slf4jLogger())
                .logLevel(feignLoggerLevel())
                .errorDecoder(errorDecoder());
    }
}