package ru.practicum.ewm.dto.comment;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentResponseDto {
    Long id;
    Long eventId;
    Long userId;
    String authorName;
    String content;
    String status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}