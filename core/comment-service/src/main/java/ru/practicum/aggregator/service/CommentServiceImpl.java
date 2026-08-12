package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.aggregator.collector.mapper.CommentMapper;
import ru.practicum.aggregator.model.Comment;
import ru.practicum.aggregator.model.CommentStatus;
import ru.practicum.aggregator.repository.CommentRepository;
import ru.practicum.client.EventClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.comment.CommentResponseDto;
import ru.practicum.dto.comment.CommentStatusUpdateRequest;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.dto.comment.UpdateCommentUserRequest;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.CommentException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Transactional
    @Override
    public CommentResponseDto addComment(Long userId, Long eventId, NewCommentDto dto) {
        log.info("Создание комментария пользователем {} к событию {}", userId, eventId);

        try {
            UserShortDto user = userClient.getUserShort(userId);
            if (user == null) {
                throw new NotFoundException(String.format("Пользователь с ID: %s не найден.", userId));
            }
        } catch (Exception e) {
            log.error("Ошибка при проверке пользователя {}: {}", userId, e.getMessage());
            throw new NotFoundException(String.format("Пользователь с ID: %s не найден или сервис недоступен.", userId));
        }

        EventFullDto event;
        try {
            event = eventClient.getEventFull(eventId);
            if (event == null) {
                throw new NotFoundException(String.format("Событие с ID: %s не найдено.", eventId));
            }

            // Проверяем статус события - только PUBLISHED можно комментировать
            if (event.getState() == null || !"PUBLISHED".equals(event.getState())) {
                throw new ValidationException(String.format("Комментарии можно оставлять только к опубликованным событиям. Текущий статус: %s", event.getState()));
            }
        } catch (Exception e) {
            log.error("Ошибка при проверке событии {}: {}", eventId, e.getMessage());
            throw new NotFoundException(String.format("Событие с ID: %s не найдено или сервис недоступен.", eventId));
        }

        // Создаем комментарий
        Comment newComment = commentMapper.toComment(dto, userId, eventId);
        Comment saved = commentRepository.save(newComment);

        log.info("Создан новый комментарий с ID: {}", saved.getId());
        return commentMapper.toResponseDto(saved);
    }

    @Transactional
    @Override
    public CommentResponseDto patchCommentById(UpdateCommentUserRequest dto) {
        log.info("Обновление комментария {} пользователем {}", dto.getId(), dto.getUserId());

        Comment comment = commentRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий с ID: %s не найден.", dto.getId())));

        if (!comment.getUserId().equals(dto.getUserId())) {
            throw new ValidationException("Пользователь не является автором комментария.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (comment.getCreatedAt().isBefore(now.minusHours(24))) {
            throw new ValidationException("Прошло более 24 часов с момента создания. Редактирование невозможно.");
        }

        if (comment.getStatus() == CommentStatus.REJECTED) {
            throw new ValidationException("Отклоненный комментарий нельзя редактировать.");
        }

        comment.setContent(dto.getContent());
        comment.setUpdatedAt(now);
        if (comment.getStatus() == CommentStatus.APPROVED) {
            comment.setStatus(CommentStatus.PENDING);
        }

        Comment updated = commentRepository.save(comment);
        log.info("Комментарий с ID: {} обновлен.", updated.getId());

        return commentMapper.toResponseDto(updated);
    }

    @Override
    public Page<CommentResponseDto> getApprovedCommentsByEvent(Long eventId, Pageable pageable) {
        log.debug("Получение подтвержденных комментариев для event: {}", eventId);

        try {
            if (!eventClient.eventExists(eventId)) {
                throw new NotFoundException(String.format("Событие с ID: %s не найдено.", eventId));
            }
        } catch (Exception e) {
            log.warn("Не удалось проверить существование события {}: {}", eventId, e.getMessage());
        }

        Page<Comment> comments = commentRepository.findByEventIdAndStatus(
                eventId, CommentStatus.APPROVED, pageable);

        return comments.map(commentMapper::toResponseDto);
    }

    @Transactional
    @Override
    public CommentResponseDto updateCommentStatus(Long commentId, CommentStatusUpdateRequest request) {
        log.info("Admin: изменить статус комментария с id={} на status - {}", commentId, request.getStatus());

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий с ID= %s не найден", commentId)));

        CommentStatus newStatus;
        try {
            newStatus = CommentStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CommentException(
                    String.format("Недопустимый статус: %s. Допустимые значения: PENDING, APPROVED, REJECTED", request.getStatus()));
        }

        if (comment.getStatus() == newStatus) {
            throw new CommentException(String.format("Комментарий уже имеет статус '%s'", newStatus));
        }

        comment.setStatus(newStatus);
        comment.setUpdatedAt(LocalDateTime.now());

        Comment updated = commentRepository.save(comment);
        log.info("Admin: статус комментария с id={} изменен на {}", commentId, newStatus);

        return commentMapper.toResponseDto(updated);
    }

    @Override
    public Page<CommentResponseDto> getCommentsByEvent(Long eventId, String status, Pageable pageable) {
        log.debug("Admin: получить комментарии по событию eventId= {}, status: {}", eventId, status);

        if (status != null && !status.isBlank()) {
            try {
                CommentStatus commentStatus = CommentStatus.valueOf(status.toUpperCase());
                return commentRepository.findByEventIdAndStatus(eventId, commentStatus, pageable)
                        .map(commentMapper::toResponseDto);
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Некорректный статус. Допустимые значения: PENDING, APPROVED, REJECTED");
            }
        }

        return commentRepository.findByEventId(eventId, pageable)
                .map(commentMapper::toResponseDto);
    }

    @Override
    @Transactional
    public void deleteAdminComment(Long commentId) {
        log.info("Admin: удалить комментарий с id: {}", commentId);

        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException(String.format("Комментарий с ID: %s не найден", commentId));
        }

        commentRepository.deleteById(commentId);
        log.info("Admin: комментарий удален, id: {}", commentId);
    }

    @Override
    @Transactional
    public void removeCommentById(Long userId, Long eventId, Long commentId) {
        log.info("Удаление комментария {} пользователем {} из события {}", commentId, userId, eventId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Комментарий с ID: %s не найден.", commentId)));

        if (!comment.getUserId().equals(userId)) {
            throw new ValidationException("Пользователь не является автором комментария.");
        }

        if (!comment.getEventId().equals(eventId)) {
            throw new ValidationException("Комментарий не принадлежит указанному событию.");
        }

        commentRepository.deleteById(commentId);
        log.info("Комментарий с ID: {} удален пользователем {}", commentId, userId);
    }
}