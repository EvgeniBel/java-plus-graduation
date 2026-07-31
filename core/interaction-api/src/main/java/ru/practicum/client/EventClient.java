package ru.practicum.client;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.*;

import java.util.List;

import static ru.practicum.constants.ApiConstants.*;

@FeignClient(name = "event-service")
public interface EventClient {

    // === АДМИНИСТРАТОР ===
    @GetMapping(EVENT_BY_ID)
    EventFullDto getEventById(@PathVariable("eventId") Long eventId);

    @GetMapping(EVENT_STATUS)
    String getEventStatus(@PathVariable("eventId") Long eventId);

    @GetMapping(EVENT_EXISTS)
    boolean eventExists(@PathVariable("eventId") Long eventId);

    // === ПУБЛИЧНЫЕ ===
    @GetMapping(EVENTS_PUBLIC)
    List<EventShortDto> getPublicEvents(@SpringQueryMap PublicEventRequestParam params);

    @GetMapping(EVENT_SHORT)
    EventShortDto getEventShort(@PathVariable("eventId") Long eventId);

    @GetMapping(EVENT_BY_ID)
    EventFullDto getEventFull(@PathVariable("eventId") Long eventId);

    // === ПОЛЬЗОВАТЕЛЬСКИЕ ===
    @PostMapping(EVENTS_USER)
    EventFullDto createEvent(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody NewEventDto eventDto
    );

    @PutMapping(EVENT_BY_ID)
    EventFullDto updateEvent(
            @PathVariable("eventId") Long eventId,
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody UpdateEventUserRequest request
    );

    @PostMapping(EVENT_CONFIRM_REQUEST)
    void confirmRequest(@PathVariable("eventId") Long eventId);
}