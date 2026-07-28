package ru.practicum.client;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.comment.CommentResponseDto;
import ru.practicum.dto.comment.CommentStatusUpdateRequest;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.dto.comment.UpdateCommentUserRequest;

import static ru.practicum.constants.ApiConstants.*;

@FeignClient(name = "comment-service")
public interface CommentClient {

    // ==================== АДМИНИСТРАТОР ====================

    @GetMapping(COMMENT_BY_ID)
    CommentResponseDto getCommentById(@PathVariable(COMMENT_BY_ID_PARAM) Long commentId);

    @PutMapping(COMMENT_MODERATE)
    CommentResponseDto moderateComment(
            @PathVariable(COMMENT_MODERATE_PARAM) Long commentId,
            @Valid @RequestBody CommentStatusUpdateRequest request
    );

    @DeleteMapping(COMMENT_BY_ID)
    void deleteComment(@PathVariable(COMMENT_BY_ID_PARAM) Long commentId);

    @GetMapping(COMMENTS_BASE)
    Page<CommentResponseDto> getAllComments(
            @RequestParam(value = FROM_PARAM, defaultValue = DEFAULT_FROM) Integer from,
            @RequestParam(value = SIZE_PARAM, defaultValue = DEFAULT_SIZE) Integer size
    );

    @GetMapping(COMMENTS_BASE + "/status")
    Page<CommentResponseDto> getCommentsByStatus(
            @RequestParam("status") String status,
            @RequestParam(value = FROM_PARAM, defaultValue = DEFAULT_FROM) Integer from,
            @RequestParam(value = SIZE_PARAM, defaultValue = DEFAULT_SIZE) Integer size
    );

    // ==================== ПУБЛИЧНЫЕ ====================

    @GetMapping(COMMENTS_BY_EVENT)
    Page<CommentResponseDto> getCommentsByEvent(
            @PathVariable(COMMENTS_BY_EVENT_PARAM) Long eventId,
            @RequestParam(value = FROM_PARAM, defaultValue = DEFAULT_FROM) Integer from,
            @RequestParam(value = SIZE_PARAM, defaultValue = DEFAULT_SIZE) Integer size
    );

    @GetMapping(COMMENT_PUBLIC_BY_ID)
    CommentResponseDto getPublicCommentById(@PathVariable(COMMENT_BY_ID_PARAM) Long commentId);

    // ==================== ПОЛЬЗОВАТЕЛЬСКИЕ ====================

    @PostMapping(COMMENTS_USER)
    CommentResponseDto createComment(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam("eventId") Long eventId,
            @Valid @RequestBody NewCommentDto dto
    );

    @PutMapping(COMMENTS_USER + "/{commentId}")
    CommentResponseDto updateComment(
            @PathVariable(COMMENT_BY_ID_PARAM) Long commentId,
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody UpdateCommentUserRequest request
    );

    @DeleteMapping(COMMENTS_USER + "/{commentId}")
    void deleteUserComment(
            @PathVariable(COMMENT_BY_ID_PARAM) Long commentId,
            @RequestHeader(USER_ID_HEADER) Long userId
    );

    @GetMapping(COMMENTS_BY_USER)
    Page<CommentResponseDto> getCommentsByUser(
            @PathVariable(COMMENTS_BY_USER_PARAM) Long userId,
            @RequestParam(value = FROM_PARAM, defaultValue = DEFAULT_FROM) Integer from,
            @RequestParam(value = SIZE_PARAM, defaultValue = DEFAULT_SIZE) Integer size
    );

    @GetMapping(COMMENTS_USER + "/{commentId}/check-author")
    boolean isCommentAuthor(
            @PathVariable(COMMENT_BY_ID_PARAM) Long commentId,
            @RequestHeader(USER_ID_HEADER) Long userId
    );

    // ==================== ВНУТРЕННИЕ (для других микросервисов) ====================

    @GetMapping(COMMENT_EXISTS_INTERNAL)
    boolean commentExists(@PathVariable(COMMENT_BY_ID_PARAM) Long commentId);

    @GetMapping(COMMENT_STATUS_INTERNAL)
    String getCommentStatus(@PathVariable(COMMENT_BY_ID_PARAM) Long commentId);
}