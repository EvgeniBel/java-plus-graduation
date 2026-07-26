package ru.practicum;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class StatClientConfig {

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(3000L); // 3 секунды между попытками
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3); // 3 попытки
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    @Bean
    public StatClient statClient(DiscoveryClient discoveryClient,
                                 RetryTemplate retryTemplate,
                                 @Value("${stat.service.id:stat-service}") String statsServiceId) {
        return new StatClientImpl(discoveryClient, retryTemplate, statsServiceId);
    }
}