package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.service.RequestService;

import static ru.practicum.constants.ApiConstants.REQUESTS_INTERNAL;
import static ru.practicum.constants.ApiConstants.REQUEST_ID_PATH;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(REQUESTS_INTERNAL)
public class RequestInternalController {

    private final RequestService requestService;

    @PostMapping(REQUEST_ID_PATH + "/status")
    public ParticipationRequestDto updateRequestStatus(
            @PathVariable Long requestId,
            @RequestParam String status
    ) {
        log.info("Post /internal/requests/{}/status?status={}", requestId, status);
        return requestService.updateRequestStatus(requestId, status);
    }
}
