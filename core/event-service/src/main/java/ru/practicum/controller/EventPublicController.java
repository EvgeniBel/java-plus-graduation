package ru.practicum.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.StatClient;
import ru.practicum.constants.Constants;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.PublicEventRequestParam;
import ru.practicum.ewm.HitDto;
import ru.practicum.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.constants.ApiConstants.EVENTS_PREFIX;

@RestController
@RequestMapping(EVENTS_PREFIX)
@RequiredArgsConstructor
@Slf4j
@Validated
public class EventPublicController {

    private final EventService eventService;
    private final StatClient statClient;

    @GetMapping
    public List<EventShortDto> getEventsByPublicRequest(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) @Pattern(regexp = "EVENT_DATE|VIEWS",
                    message = "Сортировка возможна только по EVENT_DATE или VIEWS") String sort,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size,
            HttpServletRequest request
    ) {
        log.info("Публичный запрос на получение событий: text={}, categories={}, paid={}, sort={}, from={}, size={}",
                text, categories, paid, sort, from, size);

        // Отправка статистики
        try {
            HitDto hitDto = HitDto.builder()
                    .app("event-service")
                    .uri(request.getRequestURI())
                    .ip(request.getRemoteAddr())
                    .timestamp(LocalDateTime.now().format(Constants.FORMATTER))
                    .build();
            statClient.postHit(hitDto);
        } catch (Exception e) {
            log.warn("Не удалось отправить статистику: {}", e.getMessage());
        }

        PublicEventRequestParam param = PublicEventRequestParam.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .onlyAvailable(onlyAvailable)
                .sort(sort)
                .from(from)
                .size(size)
                .build();

        return eventService.getEventsByPublicRequest(param);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEventByIdByPublicRequest(
            @PathVariable @Positive Long eventId,
            HttpServletRequest request
    ) {
        log.info("Публичный запрос на получение события с ID: {}", eventId);

        // Отправка статистики
        try {
            HitDto hitDto = HitDto.builder()
                    .app("event-service")
                    .uri(request.getRequestURI())
                    .ip(request.getRemoteAddr())
                    .timestamp(LocalDateTime.now().format(Constants.FORMATTER))
                    .build();
            statClient.postHit(hitDto);
        } catch (Exception e) {
            log.warn("Не удалось отправить статистику: {}", e.getMessage());
        }

        return eventService.getEventByIdByPublicRequest(eventId);
    }
}