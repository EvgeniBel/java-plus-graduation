package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.EventClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.event.EventFullDto;  // ✅ Используем EventFullDto
import ru.practicum.dto.request.CreateUpdateRequestDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Transactional
    @Override
    public ParticipationRequestDto createRequest(Long userId, CreateUpdateRequestDto dto) {
        log.info("Создание запроса: userId={}, eventId={}", userId, dto.getEventId());

        UserShortDto requester;
        try {
            requester = userClient.getUserShort(userId);
            if (requester == null) {
                throw new NotFoundException("Пользователь с ID=" + userId + " не найден");
            }
        } catch (Exception e) {
            log.error("Ошибка при проверке пользователя: {}", e.getMessage());
            throw new NotFoundException("Пользователь с ID=" + userId + " не найден или сервис недоступен");
        }

        EventFullDto event;
        try {
            event = eventClient.getEventFull(dto.getEventId());
            if (event == null) {
                throw new NotFoundException("Событие с ID=" + dto.getEventId() + " не найдено");
            }
        } catch (Exception e) {
            log.error("Ошибка при проверке события: {}", e.getMessage());
            throw new NotFoundException("Событие с ID=" + dto.getEventId() + " не найдено или сервис недоступен");
        }

        // Проверка, что событие опубликовано
        if (event.getState() == null || !"PUBLISHED".equals(event.getState())) {
            log.error("Не удается создать запрос на неопубликованное событие с id={}", dto.getEventId());
            throw new ConflictException("Событие еще не опубликовано. Текущий статус: " + event.getState());
        }

        // Проверка, что инициатор не пытается участвовать в своем событии
        if (event.getInitiator() != null && event.getInitiator().getId().equals(userId)) {
            log.error("Инициатор не может участвовать в собственном мероприятии. eventId={}, userId={}",
                    dto.getEventId(), userId);
            throw new ConflictException("Инициатор не может участвовать в собственном мероприятии");
        }

        // Проверка, что пользователь уже не создавал запрос
        Optional<ParticipationRequest> existingRequest =
                requestRepository.findByRequesterIdAndEventId(userId, dto.getEventId());

        if (existingRequest.isPresent()) {
            log.error("Запрос пользователя {} на событие {} уже существует", userId, dto.getEventId());
            throw new ConflictException("Запрос пользователя на это событие уже существует");
        }

        Long approvedRequestsCount = requestRepository.countByEventIdAndStatus(
                dto.getEventId(), RequestStatus.CONFIRMED);

        Integer participantLimit = event.getParticipantLimit() != null ? event.getParticipantLimit() : 0;
        Boolean requestModeration = event.getRequestModeration() != null ? event.getRequestModeration() : true;

        if (participantLimit > 0 && approvedRequestsCount >= participantLimit) {
            log.error("Достигнут лимит участников для event {}. Limit: {}, CONFIRMED: {}",
                    dto.getEventId(), participantLimit, approvedRequestsCount);
            throw new ConflictException("Достигнут лимит участников");
        }

        // Определение статуса запроса
        RequestStatus initialStatus;
        if (participantLimit == 0) {
            initialStatus = RequestStatus.CONFIRMED;
        } else if (!requestModeration) {
            initialStatus = RequestStatus.CONFIRMED;
        } else {
            initialStatus = RequestStatus.PENDING;
        }

        // Создаем запрос
        ParticipationRequest request = RequestMapper.toEntity(
                LocalDateTime.now(),
                dto.getEventId(),
                userId,
                initialStatus
        );

        ParticipationRequest saved = requestRepository.save(request);
        log.info("Создан запрос с id={}, статус={}", saved.getId(), initialStatus);
        return RequestMapper.toParticipationRequestDto(saved);
    }

    @Override
    public List<ParticipationRequestDto> getRequestByUserId(Long userId) {
        log.info("Получение запросов пользователя с id={}", userId);

        // Проверяем существование пользователя через Feign
        try {
            if (!userClient.userExists(userId)) {
                throw new NotFoundException((String.format("Пользователь с ID=%s не найден", userId)));
            }
        } catch (Exception e) {
            log.error("Ошибка при проверке пользователя: {}", e.getMessage());
            throw new NotFoundException((String.format("Пользователь с ID=%s не найден или сервис недоступен", userId)));
        }

        return requestRepository.findAllByUserId(userId)
                .stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
    }

    @Transactional
    @Override
    public ParticipationRequestDto canceledRequest(Long userId, Long requestId) {
        log.info("Отмена запроса: userId={}, requestId={}", userId, requestId);

        // Проверяем существование пользователя через Feign
        try {
            if (!userClient.userExists(userId)) {
                throw new NotFoundException((String.format("Пользователь с ID=%s не найден", userId)));
            }
        } catch (Exception e) {
            log.error("Ошибка при проверке пользователя: {}", e.getMessage());
            throw new NotFoundException((String.format("Пользователь с ID=%s не найден или сервис недоступен", userId)));
        }

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException(String.format("Запрос с ID=%s не найден", requestId)));

        // Проверка, что запрос принадлежит пользователю
        if (!request.getRequesterId().equals(userId)) {
            log.error("Запрос с id={} не принадлежит пользователю с id={}", requestId, userId);
            throw new NotFoundException("Запрос не найден или не принадлежит пользователю");
        }

        // Только PENDING запросы можно отменить
        if (request.getStatus() != RequestStatus.PENDING) {
            log.error("Нельзя отменить запрос со статусом: {}", request.getStatus());
            throw new ConflictException("Можно отменить только запросы в статусе PENDING");
        }

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest canceled = requestRepository.save(request);
        log.info("Запрос с id={} отменен", requestId);
        return RequestMapper.toParticipationRequestDto(canceled);
    }

    @Override
    public Long getConfirmedRequestsCount(Long eventId) {
        log.info("Получение количества подтвержденных запросов для события {}", eventId);
        return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByEvent(Long eventId) {
        log.info("Получение запросов для события {}", eventId);
        return requestRepository.findAllByEventId(eventId)
                .stream()
                .map(RequestMapper::toParticipationRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto updateRequestStatus(Long requestId, String status) {
        log.info("Обновление статуса запроса {} на {}", requestId, status);

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с ID=" + requestId + " не найден"));

        RequestStatus newStatus;
        try {
            newStatus = RequestStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Недопустимый статус: " + status);
        }

        // Проверка, что статус можно изменить
        if (request.getStatus() == RequestStatus.CONFIRMED && newStatus == RequestStatus.REJECTED) {
            throw new ConflictException("Нельзя отклонить уже подтвержденный запрос");
        }

        request.setStatus(newStatus);
        ParticipationRequest updated = requestRepository.save(request);
        log.info("Статус запроса {} обновлен на {}", requestId, newStatus);

        return RequestMapper.toParticipationRequestDto(updated);
    }

}