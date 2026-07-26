package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.ewm.HitDto;
import ru.practicum.ewm.StatRequestParamDto;
import ru.practicum.ewm.StatResponseDto;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
public class StatClientImpl implements StatClient {
    private final RestClient restClient;
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final String statsServiceId;
    private final String baseUrl;  // ✅ Добавляем поле для baseUrl

    public StatClientImpl(DiscoveryClient discoveryClient,
                          RetryTemplate retryTemplate,
                          String statsServiceId) {
        this.discoveryClient = discoveryClient;
        this.retryTemplate = retryTemplate;
        this.statsServiceId = statsServiceId;
        this.baseUrl = null;
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // Конструктор для тестов без DiscoveryClient
    public StatClientImpl(String baseUrl) {
        this.discoveryClient = null;
        this.retryTemplate = null;
        this.statsServiceId = null;
        this.baseUrl = baseUrl;
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // Конструктор для тестов с RestClient.Builder
    public StatClientImpl(RestClient.Builder builder, String baseUrl) {
        this.discoveryClient = null;
        this.retryTemplate = null;
        this.statsServiceId = null;
        this.baseUrl = baseUrl;
        this.restClient = builder
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private ServiceInstance getInstance() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(statsServiceId);
            if (instances.isEmpty()) {
                throw new RuntimeException("No instances found for service: " + statsServiceId);
            }
            return instances.get(0);
        } catch (Exception exception) {
            log.error("Ошибка обнаружения адреса сервиса статистики с id: {}", statsServiceId, exception);
            throw new StatsServerUnavailable(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId,
                    exception
            );
        }
    }

    private URI makeUri(String path) {
        if (discoveryClient != null && retryTemplate != null) {
            // Для production - через DiscoveryClient
            ServiceInstance instance = retryTemplate.execute(context -> getInstance());
            return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
        } else if (baseUrl != null) {
            // Для тестов - используем baseUrl
            String fullUrl = baseUrl + path;
            return URI.create(fullUrl);
        } else {
            // Fallback
            return URI.create(path);
        }
    }

    @Override
    public HitDto postHit(HitDto dto) {
        try {
            URI uri = makeUri("/hit");
            return restClient.post()
                    .uri(uri)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .body(dto)
                    .retrieve()
                    .body(HitDto.class);
        } catch (Exception e) {
            log.error("Неудачная попытка добавления записи в сервис статистики. Запись: {}", dto, e);
            return new HitDto();
        }
    }

    @Override
    public List<StatResponseDto> getStats(StatRequestParamDto dto) {
        try {
            URI baseUri = makeUri("/stats");

            URI fullUri = UriComponentsBuilder.fromUri(baseUri)
                    .queryParam("start", dto.getStart())
                    .queryParam("end", dto.getEnd())
                    .queryParam("uris", dto.getUris())
                    .queryParam("unique", dto.getUnique())
                    .build()
                    .toUri();

            return restClient.get()
                    .uri(fullUri)
                    .accept(APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<StatResponseDto>>() {});
        } catch (Exception e) {
            log.error("Неудачная попытка получения данных статистики из сервиса статистики. " +
                    "Параметры запроса: {}", dto, e);
            return new ArrayList<>();
        }
    }
}