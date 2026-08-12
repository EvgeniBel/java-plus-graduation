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
import ru.practicum.exception.ValidationException;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
@Validated
public class EventRecommendationController {

    private final RecommendationService recommendationService;
    private final EventService eventService;

    @GetMapping("/recommendations")
    public List<RecommendedEventDto> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") Long userId,
            @RequestParam(defaultValue = "10") @Positive int maxResults
    ) {
        log.info("Запрос рекомендаций: userId={}, maxResults={}", userId, maxResults);
        return recommendationService.getRecommendationsForUser(userId, maxResults);
    }

    @PutMapping("/{eventId}/like")
    @ResponseStatus(HttpStatus.OK)
    public void likeEvent(
            @PathVariable Long eventId,
            @RequestHeader("X-EWM-USER-ID") Long userId
    ) {
        log.info("Лайк: userId={}, eventId={}", userId, eventId);

        if (!eventService.hasUserVisitedEvent(userId, eventId)) {
            throw new ValidationException("Пользователь может лайкать только посещённые им мероприятия");
        }

        recommendationService.sendLikeAction(userId, eventId);
    }
}