package ru.practicum.aggregator.collector.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.client.EventClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.comment.CommentResponseDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.aggregator.model.Comment;
import ru.practicum.aggregator.model.CommentStatus;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentMapper {

    private final UserClient userClient;
    private final EventClient eventClient;

    public Comment toComment(NewCommentDto dto, Long userId, Long eventId) {
        return Comment.builder()
                .content(dto.getContent())
                .status(CommentStatus.PENDING)
                .userId(userId)
                .eventId(eventId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public CommentResponseDto toResponseDto(Comment comment) {
        CommentResponseDto.CommentResponseDtoBuilder builder = CommentResponseDto.builder()
                .id(comment.getId())
                .eventId(comment.getEventId())
                .userId(comment.getUserId())
                .content(comment.getContent())
                .status(comment.getStatus().toString())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt());

        // Получаем имя пользователя через Feign
        try {
            UserShortDto user = userClient.getUserShort(comment.getUserId());
            builder.authorName(user != null ? user.getName() : "Unknown user");
        } catch (Exception e) {
            log.warn("Не удалось получить данные пользователя для userId={}: {}",
                    comment.getUserId(), e.getMessage());
            builder.authorName("Unknown user");
        }

        return builder.build();
    }

    //Для публичных методов - без запроса к другим сервисам (быстрее)
    public CommentResponseDto toSimpleResponseDto(Comment comment) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .eventId(comment.getEventId())
                .userId(comment.getUserId())
                .authorName("User " + comment.getUserId())
                .content(comment.getContent())
                .status(comment.getStatus().toString())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}