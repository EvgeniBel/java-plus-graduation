package ru.practicum.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.comment.CommentResponseDto;
import ru.practicum.dto.comment.CommentStatusUpdateRequest;
import ru.practicum.service.CommentService;

import static ru.practicum.constants.ApiConstants.COMMENTS_BASE;


@RestController
@RequestMapping(COMMENTS_BASE)
@RequiredArgsConstructor
@Slf4j
@Validated
public class CommentAdminController {

    private final CommentService commentService;

    @PatchMapping("/{commentId}/moderate")
    public CommentResponseDto updateCommentStatus(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentStatusUpdateRequest request
    ) {
        log.info("Админ запрос: изменить статус комментария ID={} на {}", commentId, request.getStatus());
        return commentService.updateCommentStatus(commentId, request);
    }

    @GetMapping("/events/{eventId}")
    public Page<CommentResponseDto> getCommentsByEvent(
            @PathVariable Long eventId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {

        log.info("Admin запрос: получить комментарии события ID: {}. Status: {}, from: {}, size: {}",
                eventId, status, from, size);

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("createdAt").descending());
        return commentService.getCommentsByEvent(eventId, status, pageable);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAdminComment(@PathVariable Long commentId) {
        log.info("Admin запрос: удалить комментарий ID: {}", commentId);
        commentService.deleteAdminComment(commentId);
    }
}
