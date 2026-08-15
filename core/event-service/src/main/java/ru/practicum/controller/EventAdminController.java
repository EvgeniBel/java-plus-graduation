package ru.practicum.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.aggregator.service.EventService;
import ru.practicum.dto.event.AdminEventRequestParam;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.UpdateEventAdminRequest;

import java.util.List;
import java.util.stream.Collectors;

import static ru.practicum.constants.ApiConstants.EVENTS_BASE;
import static ru.practicum.constants.ApiConstants.EVENT_ID_PATH;

@RestController
@RequestMapping(EVENTS_BASE)
@RequiredArgsConstructor
@Slf4j
@Validated
public class EventAdminController {

    private final EventService eventService;

    @GetMapping
    public List<EventFullDto> getEventsByAdminRequest(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<String> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size
    ) {
        log.info("Admin запрос на получение событий: users={}, states={}, categories={}, from={}, size={}",
                users, states, categories, from, size);

        List<Long> validUsers = filterValidIds(users);
        List<Long> validCategories = filterValidIds(categories);

        AdminEventRequestParam param = AdminEventRequestParam.builder()
                .users(validUsers)
                .states(states != null && !states.isEmpty() ? states : null)
                .categories(validCategories)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .from(from)
                .size(size)
                .build();

        return eventService.getEventsByAdminRequest(param);
    }

    @PatchMapping(EVENT_ID_PATH)
    public EventFullDto patchEventByIdByAdmin(
            @PathVariable @Positive Long eventId,
            @RequestBody @NotNull @Valid UpdateEventAdminRequest dto
    ) {
        log.info("Admin обновление события с ID: {}", eventId);
        return eventService.patchEventByIdByAdmin(eventId, dto);
    }

    @GetMapping(EVENT_ID_PATH + "/full")
    public EventFullDto getEventFull(@PathVariable Long eventId) {
        log.info("Admin получение полной информации о событии с ID: {}", eventId);
        return eventService.getEventFull(eventId);
    }

    private List<Long> filterValidIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        List<Long> validIds = ids.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());
        return validIds.isEmpty() ? null : validIds;
    }
}