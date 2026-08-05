package ru.practicum.client;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.*;

import java.util.List;

import static ru.practicum.constants.ApiConstants.*;

@FeignClient(name = "event-service")
public interface EventClient {

    // === АДМИНИСТРАТОР ===
    @GetMapping(EVENT_BY_ID)
    EventFullDto getEventById(@PathVariable(EVENT_BY_ID_PARAM) Long eventId);

    @GetMapping(EVENT_STATUS)
    String getEventStatus(@PathVariable(EVENT_BY_ID_PARAM) Long eventId);

    @GetMapping(EVENT_EXISTS)
    boolean eventExists(@PathVariable(EVENT_BY_ID_PARAM) Long eventId);

    @GetMapping(EVENT_FULL)
    EventFullDto getEventFull(@PathVariable(EVENT_BY_ID_PARAM) Long eventId);

    // === ПУБЛИЧНЫЕ ===
    @GetMapping(EVENTS_PUBLIC)
    List<EventShortDto> getPublicEvents(@RequestParam(required = false) String text,
                                        @RequestParam(required = false) List<Long> categories,
                                        @RequestParam(required = false) Boolean paid,
                                        @RequestParam(required = false) String rangeStart,
                                        @RequestParam(required = false) String rangeEnd,
                                        @RequestParam(defaultValue = "false") Boolean onlyAvailable,
                                        @RequestParam(required = false) String sort,
                                        @RequestParam(defaultValue = "0") Integer from,
                                        @RequestParam(defaultValue = "10") Integer size);

    @GetMapping(EVENT_SHORT)
    EventShortDto getEventShort(@PathVariable(EVENT_BY_ID_PARAM) Long eventId);

    // === ПОЛЬЗОВАТЕЛЬСКИЕ ===
    @PostMapping(EVENTS_USER)
    EventFullDto createEvent(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody NewEventDto eventDto
    );

    @PutMapping(EVENT_BY_ID)
    EventFullDto updateEvent(
            @PathVariable(EVENT_BY_ID_PARAM) Long eventId,
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody UpdateEventUserRequest request
    );

    @PostMapping(EVENT_CONFIRM_REQUEST)
    void confirmRequest(@PathVariable(EVENT_BY_ID_PARAM) Long eventId);
}