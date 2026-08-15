package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.aggregator.service.RequestService;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;

import static ru.practicum.constants.ApiConstants.REQUESTS_BY_EVENT;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(REQUESTS_BY_EVENT)
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
