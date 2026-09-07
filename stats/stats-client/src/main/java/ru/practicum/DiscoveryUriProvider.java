package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.retry.support.RetryTemplate;

import java.net.URI;
import java.util.List;

@Slf4j
public class DiscoveryUriProvider implements UriProvider {

    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final String statsServiceId;

    public DiscoveryUriProvider(DiscoveryClient discoveryClient,
                                RetryTemplate retryTemplate,
                                @Value("${stat.service.id:stats-server}") String statsServiceId) {
        this.discoveryClient = discoveryClient;
        this.retryTemplate = retryTemplate;
        this.statsServiceId = statsServiceId;
    }

    private ServiceInstance getInstance() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(statsServiceId);
            if (instances == null || instances.isEmpty()) {
                throw new StatsServerUnavailable(
                        String.format("No instances found for service: %s", statsServiceId));
            }
            return instances.get(0);
        } catch (Exception exception) {
            log.error("Ошибка обнаружения адреса сервиса статистики с id: {}", statsServiceId, exception);
            throw new StatsServerUnavailable(
                    String.format("Ошибка обнаружения адреса сервиса статистики с id: %s", statsServiceId), exception);
        }
    }

    @Override
    public URI makeUri(String path) {
        try {
            ServiceInstance instance = retryTemplate.execute(context -> getInstance());
            return URI.create(String.format("http://%s:%d%s",
                    instance.getHost(), instance.getPort(), path));
        } catch (Exception e) {
            log.error("Ошибка создания URI для пути: {}", path, e);
            throw new StatsServerUnavailable("Не удалось создать URI для stats-server", e);
        }
    }
}