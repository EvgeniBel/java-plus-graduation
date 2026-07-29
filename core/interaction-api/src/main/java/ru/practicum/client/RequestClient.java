package ru.practicum.client;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.CreateUpdateRequestDto;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;

import static ru.practicum.constants.ApiConstants.*;

@FeignClient(name = "request-service")
public interface RequestClient {

    // ==================== ПУБЛИЧНЫЕ ====================

    @GetMapping(REQUESTS_COUNT)
    Long getConfirmedRequestsCount(@PathVariable("eventId") Long eventId);

    @GetMapping(REQUESTS_BY_EVENT)
    List<ParticipationRequestDto> getRequestsByEvent(
            @PathVariable("eventId") Long eventId
    );

    // ==================== ПОЛЬЗОВАТЕЛЬСКИЕ ====================

    @PostMapping(REQUEST_CREATE)
    ParticipationRequestDto createRequest(
            @RequestHeader(USER_ID_HEADER) Long userId,  // ✅ Добавлен userId
            @Valid @RequestBody CreateUpdateRequestDto requestDto
    );

    @PutMapping(REQUEST_CANCEL)
    ParticipationRequestDto cancelRequest(
            @RequestHeader(USER_ID_HEADER) Long userId,  // ✅ Добавлен userId
            @PathVariable("requestId") Long requestId
    );

    @GetMapping(REQUESTS_BASE)
    List<ParticipationRequestDto> getUserRequests(
            @RequestHeader(USER_ID_HEADER) Long userId
    );

    // ==================== ВНУТРЕННИЕ (для других микросервисов) ====================

    @GetMapping(REQUESTS_INTERNAL + "/event/{eventId}/count")
    Long getConfirmedRequestsCountInternal(@PathVariable("eventId") Long eventId);

    @GetMapping(REQUESTS_INTERNAL + "/event/{eventId}")
    List<ParticipationRequestDto> getRequestsByEventInternal(@PathVariable("eventId") Long eventId);

    @GetMapping(REQUESTS_INTERNAL + "/{requestId}/status")
    String getRequestStatusInternal(@PathVariable("requestId") Long requestId);
}