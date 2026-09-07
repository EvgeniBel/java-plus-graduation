package ru.practicum.aggregator.collector.config;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus status = HttpStatus.valueOf(response.status());

        log.warn("Feign ошибка при вызове {}: {}", methodKey, response.status());

        if (status.is4xxClientError()) {
            return new ResponseStatusException(status, String.format(
                    "Client error: %s (method: %s)", response.reason(), methodKey));
        } else if (status.is5xxServerError()) {
            return new ResponseStatusException(status, String.format(
                    "Server error: %s  (method: %s)", response.reason(), methodKey));
        }

        return defaultDecoder.decode(methodKey, response);
    }
}