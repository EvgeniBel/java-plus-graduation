package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.comment.CommentResponseDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.dto.comment.UpdateCommentUserRequest;
import ru.practicum.service.CommentService;


@RestController
@RequestMapping("/users/{userId}/comments")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CommentPrivateController {
    private final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponseDto addComment(
            @PathVariable Long userId,
            @RequestParam Long eventId,
            @RequestBody NewCommentDto dto
    ) {
        log.info("Создание нового комментария {} для события с ID: {} пользователем с ID: {}", dto, eventId, userId);
        return commentService.addComment(userId, eventId, dto);
    }

    @PatchMapping("/{commentId}")
    public CommentResponseDto patchCommentById(
            @PathVariable Long userId,
            @RequestParam Long eventId,
            @PathVariable Long commentId,
            @RequestBody UpdateCommentUserRequest dto
    ) {
        log.info("Обновление пользователем с ID: {} созданного им комментария с ID: {} к событию с ID: {}.",
                userId, commentId, eventId);
        dto.setId(commentId);
        dto.setUserId(userId);
        dto.setEventId(eventId);
        return commentService.patchCommentById(dto);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable Long userId,
            @RequestParam Long eventId,
            @PathVariable Long commentId
    ) {
        log.info("Удаление пользователем с ID: {} созданного им комментария с ID: {} к событию с ID: {}.",
                userId, commentId, eventId);
        commentService.removeCommentById(userId, eventId, commentId);
    }

}