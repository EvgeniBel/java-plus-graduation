package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.EventClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.request.CreateUpdateRequestDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.aggregator.collector.mapper.RequestMapper;
import ru.practicum.aggregator.model.ParticipationRequest;
import ru.practicum.aggregator.repository.RequestRepository;
import ru.practicum.grpc.CollectorGrpcClient;
import ru.practicum.stats.service.collector.UserActionOuterClass.ActionTypeProto;

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
    private final CollectorGrpcClient collectorClient;

    @Transactional
    @Override
    public ParticipationRequestDto createRequest(Long userId, CreateUpdateRequestDto dto) {
        log.info("Создание запроса: userId={}, eventId={}", userId, dto.getEventId());

        UserShortDto requester;
        try {
            requester = userClient.getUserShort(userId);
            if (requester == null) {
                throw new NotFoundException(String.format("Пользователь с ID=%s не найден", userId));
            }
        } catch (Exception e) {
            log.error("Ошибка при проверке пользователя: {}", e.getMessage());
            throw new NotFoundException(String.format("Пользователь с ID=%s не найден или сервис недоступен", userId));
        }

        EventFullDto event;
        try {
            event = eventClient.getEventFull(dto.getEventId());
            if (event == null) {
                throw new NotFoundException(String.format("Событие с ID=%s не найдено", dto.getEventId()));
            }
        } catch (Exception e) {
            log.error("Ошибка при проверке события: {}", e.getMessage());
            throw new NotFoundException(String.format("Событие с ID=%s не найдено или сервис недоступен", dto.getEventId()));
        }

        if (event.getState() == null || !"PUBLISHED".equals(event.getState())) {
            log.error("Не удается создать запрос на неопубликованное событие с id={}", dto.getEventId());
            throw new ConflictException(String.format("Событие еще не опубликовано. Текущий статус: %s", event.getState()));
        }

        if (event.getInitiator() != null && event.getInitiator().getId().equals(userId)) {
            log.error("Инициатор не может участвовать в собственном мероприятии. eventId={}, userId={}",
                    dto.getEventId(), userId);
            throw new ConflictException("Инициатор не может участвовать в собственном мероприятии");
        }

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

        log.info("participantLimit = {}, requestModeration = {}, approvedRequestsCount = {}",
                participantLimit, requestModeration, approvedRequestsCount);

        if (participantLimit > 0 && approvedRequestsCount >= participantLimit) {
            log.error("Достигнут лимит участников для event {}. Limit: {}, CONFIRMED: {}",
                    dto.getEventId(), participantLimit, approvedRequestsCount);
            throw new ConflictException("Достигнут лимит участников");
        }

        RequestStatus initialStatus;
        if (participantLimit == 0) {
            initialStatus = RequestStatus.CONFIRMED;
        } else if (!requestModeration) {
            initialStatus = RequestStatus.CONFIRMED;
        } else {
            initialStatus = RequestStatus.PENDING;
        }

        ParticipationRequest request = RequestMapper.toEntity(
                LocalDateTime.now(),
                dto.getEventId(),
                userId,
                initialStatus
        );

        ParticipationRequest saved = requestRepository.save(request);
        log.info("Создан запрос с id={}, статус={}", saved.getId(), initialStatus);

        if (saved.getStatus() == RequestStatus.CONFIRMED) {
            sendRegistrationToCollector(userId, dto.getEventId());
        }

        return RequestMapper.toParticipationRequestDto(saved);
    }

    /**
     * Отправка информации о регистрации в Collector
     */
    private void sendRegistrationToCollector(Long userId, Long eventId) {
        try {
            log.info("Отправка регистрации в Collector: userId={}, eventId={}", userId, eventId);
            boolean success = collectorClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
            if (success) {
                log.info("Регистрация отправлена: userId={}, eventId={}", userId, eventId);
            } else {
                log.warn("Не удалось отправить регистрацию: userId={}, eventId={}", userId, eventId);
            }
        } catch (Exception e) {
            log.error("Ошибка отправки регистрации: userId={}, eventId={}", userId, eventId, e);
        }
    }

    @Override
    public List<ParticipationRequestDto> getRequestByUserId(Long userId) {
        log.info("Получение запросов пользователя с id={}", userId);

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

        if (!request.getRequesterId().equals(userId)) {
            log.error("Запрос с id={} не принадлежит пользователю с id={}", requestId, userId);
            throw new NotFoundException("Запрос не найден или не принадлежит пользователю");
        }

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
                .orElseThrow(() -> new NotFoundException(String.format("Запрос с ID=%s не найден", requestId)));

        RequestStatus newStatus;
        try {
            newStatus = RequestStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(String.format("Недопустимый статус: %s", status));
        }

        if (request.getStatus() == RequestStatus.CONFIRMED && newStatus == RequestStatus.REJECTED) {
            throw new ConflictException("Нельзя отклонить уже подтвержденный запрос");
        }

        request.setStatus(newStatus);
        ParticipationRequest updated = requestRepository.save(request);
        log.info("Статус запроса {} обновлен на {}", requestId, newStatus);

        if (newStatus == RequestStatus.CONFIRMED) {
            sendRegistrationToCollector(request.getRequesterId(), request.getEventId());
        }

        return RequestMapper.toParticipationRequestDto(updated);
    }
}