package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.service.RequestService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/public/events/{eventId}/requests")
public class RequestPublicController {

    private final RequestService requestService;

    @GetMapping("/count")
    public Long getConfirmedRequestsCount(@PathVariable Long eventId) {
        log.info("GET /public/events/{}/requests/count", eventId);
        return requestService.getConfirmedRequestsCount(eventId);
    }

    @GetMapping
    public List<ParticipationRequestDto> getRequestsByEvent(@PathVariable Long eventId) {
        log.info("GET /public/events/{}/requests", eventId);
        return requestService.getRequestsByEvent(eventId);
    }
}
