package ru.practicum.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.aggregator.service.EventService;
import ru.practicum.aggregator.service.RecommendationService;
import ru.practicum.dto.event.RecommendedEventDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;

import java.util.List;

import static ru.practicum.constants.ApiConstants.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(EVENTS_PREFIX)
@Validated
public class EventRecommendationController {

    private final RecommendationService recommendationService;
    private final EventService eventService;

    @GetMapping(RECOMMENDATION_PREFIX)
    public List<RecommendedEventDto> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") Long userId,
            @RequestParam(defaultValue = "10") @Positive int maxResults
    ) {
        if (userId == null || userId <= 0) {
            throw new ValidationException("Невалидный userId: " + userId);
        }
        log.info("Запрос рекомендаций: userId={}, maxResults={}", userId, maxResults);
        return recommendationService.getRecommendationsForUser(userId, maxResults);
    }

    @GetMapping("/{eventId}/similar")
    public List<RecommendedEventDto> getSimilarEvents(
            @PathVariable Long eventId,
            @RequestHeader(value = "X-EWM-USER-ID", required = false) Long userId,
            @RequestParam(defaultValue = "10") @Positive int maxResults
    ) {
        if (eventId == null || eventId <= 0) {
            throw new ValidationException("Невалидный eventId: " + eventId);
        }
        log.info("Запрос похожих событий: eventId={}, userId={}, maxResults={}", eventId, userId, maxResults);
        return recommendationService.getSimilarEvents(eventId, userId, maxResults);
    }

    @PutMapping(EVENT_LIKE)
    @ResponseStatus(HttpStatus.OK)
    public void likeEvent(
            @PathVariable Long eventId,
            @RequestHeader("X-EWM-USER-ID") Long userId
    ) {
        if (userId == null || userId <= 0) {
            throw new ValidationException("Невалидный userId: " + userId);
        }
        if (eventId == null || eventId <= 0) {
            throw new ValidationException("Невалидный eventId: " + eventId);
        }

        log.info("Лайк: userId={}, eventId={}", userId, eventId);

        // Проверка существования события
        if (!eventService.eventExists(eventId)) {
            throw new NotFoundException(String.format("Событие с ID: %s не найдено", eventId));
        }

        // Проверка: пользователь может лайкать только посещённые им мероприятия
        if (!eventService.hasUserVisitedEvent(userId, eventId)) {
            throw new ValidationException("Пользователь может лайкать только посещённые им мероприятия");
        }

        recommendationService.sendLikeAction(userId, eventId);
    }
}