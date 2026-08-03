package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.service.RequestService;

import static ru.practicum.constants.ApiConstants.REQUEST_STATUS_UPDATE;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/requests")
public class RequestInternalController {

    private final RequestService requestService;

    @PostMapping("/{requestId}/status")
    public ParticipationRequestDto updateRequestStatus(
            @PathVariable Long requestId,
            @RequestParam String status
    ) {
        log.info("PATCH /internal/requests/{}/status?status={}", requestId, status);
        return requestService.updateRequestStatus(requestId, status);
    }
}
