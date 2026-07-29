package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.CreateUpdateRequestDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.service.RequestService;

import java.util.List;

import static ru.practicum.constants.ApiConstants.USER_ID_HEADER;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user/requests")
public class RequestPrivateController {

    private final RequestService requestService;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody CreateUpdateRequestDto dto
    ) {
        log.info("POST /user/requests/create - создание запроса пользователем {}", userId);
        return requestService.createRequest(userId, dto);
    }

    @GetMapping
    public List<ParticipationRequestDto> getRequestByUserId(
            @RequestHeader(USER_ID_HEADER) Long userId
    ) {
        log.info("GET /user/requests - получение запросов пользователя {}", userId);
        return requestService.getRequestByUserId(userId);
    }

    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto canceledRequest(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable("requestId") Long requestId
    ) {
        log.info("PATCH /user/requests/{}/cancel - отмена запроса пользователем {}", requestId, userId);
        return requestService.canceledRequest(userId, requestId);
    }
}