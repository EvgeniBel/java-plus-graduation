package ru.practicum.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.aggregator.service.RecommendationService;
import ru.practicum.dto.event.RecommendedEventDto;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
@Validated
public class EventRecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/recommendations")
    public List<RecommendedEventDto> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") Long userId,
            @RequestParam(defaultValue = "10") @Positive int maxResults
    ) {
        log.info("Запрос рекомендаций: userId={}, maxResults={}", userId, maxResults);
        return recommendationService.getRecommendationsForUser(userId, maxResults);
    }

    @PutMapping("/{eventId}/like")
    public void likeEvent(
            @PathVariable Long eventId,
            @RequestHeader("X-EWM-USER-ID") Long userId
    ) {
        log.info("Лайк: userId={}, eventId={}", userId, eventId);
        recommendationService.sendLikeAction(userId, eventId);
    }
}
