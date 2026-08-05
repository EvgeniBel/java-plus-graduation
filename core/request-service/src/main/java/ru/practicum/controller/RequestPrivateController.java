package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.CreateUpdateRequestDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.service.RequestService;

import java.util.List;

import static ru.practicum.constants.ApiConstants.REQUESTS_BASE;
import static ru.practicum.constants.ApiConstants.REQUEST_ID_PATH;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(REQUESTS_BASE)
public class RequestPrivateController {

    private final RequestService requestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(
            @PathVariable Long userId,
            @RequestParam Long eventId
    ) {
        log.info("POST /users/{}/requests?eventId={} - создание запроса пользователем {}", userId, eventId);

        CreateUpdateRequestDto dto = CreateUpdateRequestDto.builder()
                .userId(userId)
                .eventId(eventId)
                .build();

        return requestService.createRequest(userId, dto);
    }

    @GetMapping
    public List<ParticipationRequestDto> getRequestByUserId(
            @PathVariable Long userId
    ) {
        log.info("GET /users/{}/requests - получение запросов пользователя", userId);
        return requestService.getRequestByUserId(userId);
    }

    @PatchMapping(REQUEST_ID_PATH + "/cancel")
    public ParticipationRequestDto canceledRequest(
            @PathVariable Long userId,
            @PathVariable("requestId") Long requestId
    ) {
        log.info("PATCH /users/{}/requests/{}/cancel - отмена запроса пользователем {}", userId, requestId);
        return requestService.canceledRequest(userId, requestId);
    }
}