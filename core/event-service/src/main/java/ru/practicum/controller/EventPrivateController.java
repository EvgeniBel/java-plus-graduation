package ru.practicum.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.aggregator.service.EventService;

import java.util.List;

import static ru.practicum.constants.ApiConstants.*;

@RestController
@RequestMapping(EVENTS_USER)
@RequiredArgsConstructor
@Slf4j
@Validated
public class EventPrivateController {

    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(
            @PathVariable @Positive Long userId,
            @RequestBody @NotNull @Valid NewEventDto dto
    ) {
        log.info("Создание события пользователем с ID: {}", userId);

        // Устанавливаем значения по умолчанию
        if (dto.getPaid() == null) {
            dto.setPaid(false);
        }
        if (dto.getParticipantLimit() == null) {
            dto.setParticipantLimit(0);
        }
        if (dto.getRequestModeration() == null) {
            dto.setRequestModeration(true);
        }

        return eventService.addEvent(userId, dto);
    }

    @GetMapping
    public List<EventShortDto> getEventsOfUser(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size
    ) {
        log.info("Получение событий пользователя с ID: {}, from={}, size={}", userId, from, size);
        return eventService.getEventsOfUser(userId, from, size);
    }

    @GetMapping(EVENT_ID_PATH)
    public EventFullDto getEventById(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId
    ) {
        log.info("Получение события с ID: {} пользователем с ID: {}", eventId, userId);
        return eventService.getEventById(userId, eventId);
    }

    @PatchMapping(EVENT_ID_PATH)
    public EventFullDto patchEventById(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId,
            @RequestBody @NotNull @Valid UpdateEventUserRequest dto
    ) {
        log.info("Обновление события с ID: {} пользователем с ID: {}", eventId, userId);
        return eventService.patchEventById(userId, eventId, dto);
    }

    @GetMapping(EVENT_ID_REQUESTS)
    public List<ParticipationRequestDto> getRequestsOfEvent(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId
    ) {
        log.info("Получение запросов на участие в событии с ID: {} пользователем с ID: {}", eventId, userId);
        return eventService.getRequestsOfEvent(userId, eventId);
    }

    @PatchMapping(EVENT_ID_REQUESTS)
    public EventRequestStatusUpdateResult patchRequestsStatusOfEvent(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId,
            @RequestBody @NotNull @Valid EventRequestStatusUpdateRequest dto
    ) {
        log.info("Обновление статусов запросов на участие в событии с ID: {} пользователем с ID: {}", eventId, userId);
        return eventService.patchRequestsStatusOfEvent(userId, eventId, dto);
    }
}