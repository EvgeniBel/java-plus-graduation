package ru.practicum.client;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.client.fallback.RequestClientFallback;
import ru.practicum.config.FeignConfig;
import ru.practicum.dto.request.CreateUpdateRequestDto;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;

import static ru.practicum.constants.ApiConstants.*;

@FeignClient(
        name = "request-service",
        fallback = RequestClientFallback.class,
        configuration = FeignConfig.class
)
public interface RequestClient {

    @GetMapping(REQUESTS_COUNT)
    Long getConfirmedRequestsCount(@PathVariable("eventId") Long eventId);

    @GetMapping(REQUESTS_BY_EVENT)
    List<ParticipationRequestDto> getRequestsByEvent(
            @PathVariable("eventId") Long eventId
    );

    @PostMapping(REQUEST_CREATE)
    ParticipationRequestDto createRequest(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody CreateUpdateRequestDto requestDto
    );

    @PatchMapping(REQUEST_CANCEL)
    ParticipationRequestDto cancelRequest(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable("requestId") Long requestId
    );

    @GetMapping(REQUESTS_BASE)
    List<ParticipationRequestDto> getUserRequests(
            @RequestHeader(USER_ID_HEADER) Long userId
    );

    @PatchMapping(REQUEST_STATUS_UPDATE)
    ParticipationRequestDto updateRequestStatus(
            @PathVariable("requestId") Long requestId,
            @RequestParam("status") String status
    );
}