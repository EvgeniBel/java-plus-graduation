package ru.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.retry.support.RetryTemplate;

import java.net.URI;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class DiscoveryUriProvider implements UriProvider {
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;

    @Value("${stat.service.id:stats-service}")
    private final String statsServiceId;

    private ServiceInstance getInstance() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(statsServiceId);
            if (instances.isEmpty()) {
                throw new StatsServerUnavailable("No instances found for service: " + statsServiceId);
            }
            return instances.getFirst();
        } catch (Exception exception) {
            log.error("Ошибка обнаружения адреса сервиса статистики с id: {}", statsServiceId, exception);
            throw new StatsServerUnavailable(
                    String.format("Ошибка обнаружения адреса сервиса статистики с id: %s", statsServiceId), exception);
        }
    }

    @Override
    public URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(context -> getInstance());
        return URI.create(String.format("http://%s:%d%s", instance.getHost(), instance.getPort(), path));
    }
}