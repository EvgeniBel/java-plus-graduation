package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatClient;
import ru.practicum.client.RequestClient;
import ru.practicum.client.UserClient;
import ru.practicum.constants.Constants;
import ru.practicum.dto.event.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.ewm.StatRequestParamDto;
import ru.practicum.ewm.StatResponseDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.CreationRulesException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.LocationMapper;
import ru.practicum.model.*;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.LocationRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final CategoryRepository categoryRepository;
    private final UserClient userClient;
    private final RequestClient requestClient;
    private final StatClient statClient;

    // ==================== ОСНОВНЫЕ МЕТОДЫ ====================

    @Transactional
    @Override
    public EventFullDto addEvent(Long userId, NewEventDto dto) {
        log.info("Создание события пользователем {}", userId);

        // Проверяем пользователя через Feign
        UserShortDto initiator;
        try {
            initiator = userClient.getUserShort(userId);
            if (initiator == null) {
                throw new NotFoundException("Пользователь с ID: " + userId + " не найден.");
            }
        } catch (Exception e) {
            log.error("Ошибка при проверке пользователя: {}", e.getMessage());
            throw new NotFoundException("Пользователь с ID: " + userId + " не найден или сервис недоступен.");
        }

        Category category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с ID: " + dto.getCategory() + " не найдена."));

        Location location = locationRepository.save(LocationMapper.dtoToLocation(dto.getLocation()));

        LocalDateTime eventDate = LocalDateTime.parse(dto.getEventDate(), Constants.FORMATTER);

        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Время начала события должно быть не ранее, чем через два часа от текущего момента.");
        }

        Event newEvent = EventMapper.dtoToEvent(
                dto,
                category.getId(),
                LocalDateTime.now(),
                userId,
                location,
                null,
                EventState.PENDING
        );

        Event addedEvent = eventRepository.save(newEvent);
        log.info("Создано событие с ID: {}", addedEvent.getId());

        return EventMapper.eventToFullDto(addedEvent, initiator, category, 0L, 0L);
    }

    @Override
    public List<EventShortDto> getEventsOfUser(Long userId, Integer from, Integer size) {
        log.info("Получение событий пользователя: userId={}, from={}, size={}", userId, from, size);

        // Проверяем пользователя через Feign
        try {
            if (!userClient.userExists(userId)) {
                throw new NotFoundException("Пользователь с ID: " + userId + " не найден.");
            }
        } catch (Exception e) {
            log.error("Ошибка при проверке пользователя: {}", e.getMessage());
            throw new NotFoundException("Пользователь с ID: " + userId + " не найден или сервис недоступен.");
        }

        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAllByInitiatorIdOrderByEventDateAsc(userId, pageable);

        if (events.isEmpty()) {
            log.info("События для пользователя {} не найдены", userId);
            return new ArrayList<>();
        }

        // Получаем дополнительные данные
        Map<Long, Long> confirmedRequestsCount = getConfirmedRequestsCount(events);
        Map<Long, Long> viewsStats = getViewsCount(events);
        Map<Long, Category> categories = getCategories(events);
        Map<Long, UserShortDto> users = getUsers(events);

        List<EventShortDto> result = new ArrayList<>();
        for (Event event : events) {
            Long views = viewsStats.getOrDefault(event.getId(), 0L);
            Long confirmedRequests = confirmedRequestsCount.getOrDefault(event.getId(), 0L);
            Category category = categories.get(event.getCategoryId());
            UserShortDto user = users.get(event.getInitiatorId());

            EventShortDto eventShortDto = EventMapper.eventToShortDto(
                    event, user, category, confirmedRequests, views);
            result.add(eventShortDto);
        }
        return result;
    }

    @Override
    public EventFullDto getEventById(Long userId, Long eventId) {
        log.info("Получение события {} пользователем {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID: " + eventId + " не найдено."));

        // Проверяем, что пользователь является инициатором
        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Пользователь с ID: " + userId +
                    " не является инициатором события с ID: " + eventId);
        }

        // Получаем данные
        UserShortDto initiator = getUser(event.getInitiatorId());
        Category category = getCategory(event.getCategoryId());
        Long views = getViewsCount(event);
        Long confirmedRequests = getConfirmedRequestsCount(event);

        return EventMapper.eventToFullDto(event, initiator, category, confirmedRequests, views);
    }

    @Override
    public List<EventShortDto> getShortEventsInfoByIds(List<Long> eventIds) {
        log.info("Получение краткой информации о событиях по ID: {}", eventIds);

        if (eventIds == null || eventIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Event> events = eventRepository.findAllByIdInOrderByIdAsc(eventIds);

        if (events.isEmpty()) {
            log.info("События по указанным ID не найдены");
            return new ArrayList<>();
        }

        Map<Long, Long> confirmedRequestsCount = getConfirmedRequestsCount(events);
        Map<Long, Long> viewsStats = getViewsCount(events);
        Map<Long, Category> categories = getCategories(events);
        Map<Long, UserShortDto> users = getUsers(events);

        List<EventShortDto> result = new ArrayList<>();
        for (Event event : events) {
            Long views = viewsStats.getOrDefault(event.getId(), 0L);
            Long confirmedRequests = confirmedRequestsCount.getOrDefault(event.getId(), 0L);
            Category category = categories.get(event.getCategoryId());
            UserShortDto user = users.get(event.getInitiatorId());

            EventShortDto eventShortDto = EventMapper.eventToShortDto(
                    event, user, category, confirmedRequests, views);
            result.add(eventShortDto);
        }
        return result;
    }

    @Transactional
    @Override
    public EventFullDto patchEventById(Long userId, Long eventId, UpdateEventUserRequest dto) {
        log.info("Обновление события {} пользователем {}", eventId, userId);

        Event oldEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID: " + eventId + " не найдено."));

        // Проверяем, что пользователь является инициатором
        if (!oldEvent.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Пользователь с ID: " + userId +
                    " не является инициатором события с ID: " + eventId);
        }

        // Проверяем статус
        if (oldEvent.getState().equals(EventState.PUBLISHED)) {
            throw new CreationRulesException("Изменить можно только отмененные события " +
                    "или события в состоянии ожидания модерации.");
        }

        // Обновляем поля
        if (dto.getEventDate() != null) {
            LocalDateTime eventDate = LocalDateTime.parse(dto.getEventDate(), Constants.FORMATTER);
            if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException("Время начала события должно быть не ранее, " +
                        "чем через два часа от текущего момента.");
            }
            oldEvent.setEventDate(eventDate);
        }

        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с ID: " + dto.getCategory() + " не найдена."));
            oldEvent.setCategoryId(category.getId());
        }

        if (dto.getLocation() != null) {
            oldEvent.getLocation().setLat(dto.getLocation().getLat());
            oldEvent.getLocation().setLon(dto.getLocation().getLon());
        }

        if (dto.getAnnotation() != null) {
            oldEvent.setAnnotation(dto.getAnnotation());
        }

        if (dto.getDescription() != null) {
            oldEvent.setDescription(dto.getDescription());
        }

        if (dto.getPaid() != null) {
            oldEvent.setPaid(dto.getPaid());
        }

        if (dto.getParticipantLimit() != null) {
            oldEvent.setParticipantLimit(dto.getParticipantLimit());
        }

        if (dto.getRequestModeration() != null) {
            oldEvent.setRequestModeration(dto.getRequestModeration());
        }

        if (dto.getTitle() != null) {
            oldEvent.setTitle(dto.getTitle());
        }

        // Обновляем статус
        if (dto.getStateAction() != null) {
            if (dto.getStateAction().equals(UserStateAction.SEND_TO_REVIEW.toString())) {
                oldEvent.setState(EventState.PENDING);
            } else if (dto.getStateAction().equals(UserStateAction.CANCEL_REVIEW.toString())) {
                oldEvent.setState(EventState.CANCELED);
            }
        }

        Event patchedEvent = eventRepository.save(oldEvent);
        log.info("Событие {} обновлено, новый статус: {}", eventId, patchedEvent.getState());

        // Получаем данные для ответа
        UserShortDto initiator = getUser(patchedEvent.getInitiatorId());
        Category category = getCategory(patchedEvent.getCategoryId());
        Long views = getViewsCount(patchedEvent);
        Long confirmedRequests = getConfirmedRequestsCount(patchedEvent);

        return EventMapper.eventToFullDto(patchedEvent, initiator, category, confirmedRequests, views);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsOfEvent(Long userId, Long eventId) {
        log.info("Получение запросов на участие в событии {} пользователем {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID: " + eventId + " не найдено."));

        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Пользователь с ID: " + userId +
                    " не является инициатором события с ID: " + eventId);
        }

        // Получаем запросы через Feign клиент
        try {
            return requestClient.getRequestsByEvent(eventId);
        } catch (Exception e) {
            log.error("Ошибка при получении запросов: {}", e.getMessage());
            throw new NotFoundException("Не удалось получить запросы на участие.");
        }
    }

    @Transactional
    @Override
    public EventRequestStatusUpdateResult patchRequestsStatusOfEvent(Long userId, Long eventId,
                                                                     EventRequestStatusUpdateRequest dto) {
        log.info("Обновление статусов запросов для события {} пользователем {}", eventId, userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID: " + eventId + " не найдено."));

        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Пользователь с ID: " + userId +
                    " не является инициатором события с ID: " + eventId);
        }

        // Получаем запросы через Feign клиент
        List<ParticipationRequestDto> requests;
        try {
            requests = requestClient.getRequestsByEvent(eventId);
        } catch (Exception e) {
            log.error("Ошибка при получении запросов: {}", e.getMessage());
            throw new NotFoundException("Не удалось получить запросы на участие.");
        }

        // Фильтруем запросы по ID
        List<ParticipationRequestDto> targetRequests = requests.stream()
                .filter(r -> dto.getRequestIds().contains(r.getId()))
                .toList();

        if (targetRequests.size() != dto.getRequestIds().size()) {
            throw new NotFoundException("Некоторые запросы не найдены.");
        }

        // Проверяем, что все запросы в статусе PENDING
        for (ParticipationRequestDto request : targetRequests) {
            if (!"PENDING".equals(request.getStatus())) {
                throw new CreationRulesException("Статус можно изменить только у заявок, " +
                        "находящихся в состоянии ожидания.");
            }
        }

        // Получаем количество подтвержденных запросов
        Long approvedRequestsCount;
        try {
            approvedRequestsCount = requestClient.getConfirmedRequestsCount(eventId);
        } catch (Exception e) {
            log.error("Ошибка при получении количества подтвержденных запросов: {}", e.getMessage());
            throw new NotFoundException("Не удалось получить количество запросов.");
        }

        Long participantLimit = event.getParticipantLimit().longValue();

        if (participantLimit > 0 && approvedRequestsCount >= participantLimit) {
            throw new ConflictException("Достигнут лимит участников события");
        }

        List<ParticipationRequestDto> approvedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        long currentApproved = approvedRequestsCount;

        for (ParticipationRequestDto request : targetRequests) {
            if (dto.getStatus() == RequestStatus.REJECTED) {
                // Отклоняем запрос
                rejectedRequests.add(request);
            } else if (currentApproved < participantLimit || participantLimit == 0) {
                // Подтверждаем запрос
                approvedRequests.add(request);
                currentApproved++;
            } else {
                // Отклоняем, если лимит достигнут
                rejectedRequests.add(request);
            }
        }

        log.info("Подтверждено запросов: {}, отклонено: {}", approvedRequests.size(), rejectedRequests.size());

        return new EventRequestStatusUpdateResult(approvedRequests, rejectedRequests);
    }

    @Override
    public List<EventFullDto> getEventsByAdminRequest(AdminEventRequestParam param) {
        log.info("Admin поиск событий: {}", param);

        Pageable pageable = PageRequest.of(param.getFrom() / param.getSize(), param.getSize());
        List<Event> events = eventRepository.findByAdminRequest(param, pageable);

        if (events.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, Long> confirmedRequestsCount = getConfirmedRequestsCount(events);
        Map<Long, Long> viewsStats = getViewsCount(events);
        Map<Long, Category> categories = getCategories(events);
        Map<Long, UserShortDto> users = getUsers(events);

        List<EventFullDto> result = new ArrayList<>();
        for (Event event : events) {
            Long views = viewsStats.getOrDefault(event.getId(), 0L);
            Long confirmedRequests = confirmedRequestsCount.getOrDefault(event.getId(), 0L);
            Category category = categories.get(event.getCategoryId());
            UserShortDto user = users.get(event.getInitiatorId());

            EventFullDto eventFullDto = EventMapper.eventToFullDto(
                    event, user, category, confirmedRequests, views);
            result.add(eventFullDto);
        }
        return result;
    }

    @Transactional
    @Override
    public EventFullDto patchEventByIdByAdmin(Long eventId, UpdateEventAdminRequest dto) {
        log.info("Admin обновление события {}", eventId);

        Event oldEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID: " + eventId + " не найдено."));

        // Обработка изменения статуса
        if (dto.getStateAction() != null) {
            if (dto.getStateAction().equals(AdminStateAction.PUBLISH_EVENT.toString())
                    && oldEvent.getState().equals(EventState.PENDING)) {
                oldEvent.setState(EventState.PUBLISHED);
                oldEvent.setPublishedOn(LocalDateTime.now());
            } else if (dto.getStateAction().equals(AdminStateAction.REJECT_EVENT.toString())
                    && !oldEvent.getState().equals(EventState.PUBLISHED)) {
                oldEvent.setState(EventState.CANCELED);
            } else {
                throw new CreationRulesException("Опубликовать можно только событие, ожидающее публикации. " +
                        "Отклонить можно только событие, которое не опубликовано.");
            }
        }

        // Обновление даты
        if (dto.getEventDate() != null) {
            LocalDateTime eventDate = LocalDateTime.parse(dto.getEventDate(), Constants.FORMATTER);
            if (eventDate.isBefore(LocalDateTime.now().plusHours(1))) {
                throw new ValidationException("Время начала события должно быть не ранее, " +
                        "чем через час от текущего момента.");
            }
            oldEvent.setEventDate(eventDate);
        }

        // Обновление категории
        if (dto.getCategory() != null) {
            Category category = categoryRepository.findById(dto.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с ID: " + dto.getCategory() + " не найдена."));
            oldEvent.setCategoryId(category.getId());
        }

        // Обновление локации
        if (dto.getLocation() != null) {
            if (oldEvent.getLocation() != null) {
                oldEvent.getLocation().setLat(dto.getLocation().getLat());
                oldEvent.getLocation().setLon(dto.getLocation().getLon());
            } else {
                Location newLocation = locationRepository.save(LocationMapper.dtoToLocation(dto.getLocation()));
                oldEvent.setLocation(newLocation);
            }
        }

        // Обновление других полей
        if (dto.getAnnotation() != null) {
            oldEvent.setAnnotation(dto.getAnnotation());
        }

        if (dto.getDescription() != null) {
            oldEvent.setDescription(dto.getDescription());
        }

        if (dto.getPaid() != null) {
            oldEvent.setPaid(dto.getPaid());
        }

        if (dto.getParticipantLimit() != null) {
            oldEvent.setParticipantLimit(dto.getParticipantLimit());
        }

        if (dto.getRequestModeration() != null) {
            oldEvent.setRequestModeration(dto.getRequestModeration());
        }

        if (dto.getTitle() != null) {
            oldEvent.setTitle(dto.getTitle());
        }

        Event patchedEvent = eventRepository.save(oldEvent);
        log.info("Событие {} обновлено администратором, статус: {}", eventId, patchedEvent.getState());

        // Получаем данные для ответа
        UserShortDto initiator = getUser(patchedEvent.getInitiatorId());
        Category category = getCategory(patchedEvent.getCategoryId());
        Long views = getViewsCount(patchedEvent);
        Long confirmedRequests = getConfirmedRequestsCount(patchedEvent);

        return EventMapper.eventToFullDto(patchedEvent, initiator, category, confirmedRequests, views);
    }

    @Override
    public List<EventShortDto> getEventsByPublicRequest(PublicEventRequestParam param) {
        log.info("Публичный поиск событий: {}", param);

        Pageable pageable = PageRequest.of(param.getFrom() / param.getSize(), param.getSize());
        List<Event> events = eventRepository.findByPublicRequest(param, pageable);

        if (events.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, Long> confirmedRequestsCount = getConfirmedRequestsCount(events);
        Map<Long, Long> viewsStats = getViewsCount(events);
        Map<Long, Category> categories = getCategories(events);
        Map<Long, UserShortDto> users = getUsers(events);

        // Фильтрация по availability
        if (Boolean.TRUE.equals(param.getOnlyAvailable())) {
            events = events.stream()
                    .filter(event -> event.getParticipantLimit() == 0 ||
                            event.getParticipantLimit() > confirmedRequestsCount.getOrDefault(event.getId(), 0L))
                    .toList();
        }

        List<EventShortDto> result = new ArrayList<>();
        for (Event event : events) {
            Long views = viewsStats.getOrDefault(event.getId(), 0L);
            Long confirmedRequests = confirmedRequestsCount.getOrDefault(event.getId(), 0L);
            Category category = categories.get(event.getCategoryId());
            UserShortDto user = users.get(event.getInitiatorId());

            EventShortDto eventShortDto = EventMapper.eventToShortDto(
                    event, user, category, confirmedRequests, views);
            result.add(eventShortDto);
        }

        String sort = param.getSort();
        if (sort != null && sort.equalsIgnoreCase("VIEWS")) {
            return result.stream()
                    .sorted(Comparator.comparingLong(EventShortDto::getViews).reversed())
                    .toList();
        }
        return result;
    }

    @Override
    public EventFullDto getEventByIdByPublicRequest(Long eventId) {
        log.info("Публичное получение события {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID: " + eventId + " не найдено."));

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotFoundException("Можно получить данные только опубликованного события.");
        }

        UserShortDto initiator = getUser(event.getInitiatorId());
        Category category = getCategory(event.getCategoryId());
        Long views = getViewsCount(event);
        Long confirmedRequests = getConfirmedRequestsCount(event);

        return EventMapper.eventToFullDto(event, initiator, category, confirmedRequests, views);
    }

    // ==================== МЕТОДЫ ДЛЯ FEIGN КЛИЕНТОВ ====================

    @Override
    public boolean eventExists(Long eventId) {
        return eventRepository.existsById(eventId);
    }

    @Override
    public EventFullDto getEventFull(Long eventId) {
        log.info("Получение полной информации о событии {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID: " + eventId + " не найдено."));

        UserShortDto initiator = getUser(event.getInitiatorId());
        Category category = getCategory(event.getCategoryId());
        Long views = getViewsCount(event);
        Long confirmedRequests = getConfirmedRequestsCount(event);

        return EventMapper.eventToFullDto(event, initiator, category, confirmedRequests, views);
    }

    @Override
    public EventShortDto getEventShort(Long eventId) {
        log.info("Получение краткой информации о событии {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID: " + eventId + " не найдено."));

        UserShortDto initiator = getUser(event.getInitiatorId());
        Category category = getCategory(event.getCategoryId());
        Long views = getViewsCount(event);
        Long confirmedRequests = getConfirmedRequestsCount(event);

        return EventMapper.eventToShortDto(event, initiator, category, confirmedRequests, views);
    }

    @Override
    public String getEventStatus(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID: " + eventId + " не найдено."));
        return event.getState().toString();
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================


    //  Метод для получения пользователя с Fallback
    private UserShortDto getUser(Long userId) {
        try {
            return userClient.getUserShort(userId);
        } catch (Exception e) {
            log.warn("Не удалось получить пользователя {}: {}", userId, e.getMessage());
            UserShortDto defaultUser = new UserShortDto();
            defaultUser.setId(userId);
            defaultUser.setName("Unknown User");
            return defaultUser;
        }
    }

    // Метод для получения количества запросов с Fallback
    private Long getConfirmedRequestsCount(Event event) {
        try {
            Long count = requestClient.getConfirmedRequestsCount(event.getId());
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("Не удалось получить количество запросов для события {}: {}, возвращаем 0",
                    event.getId(), e.getMessage());
            return 0L; // ⚠️ Fallback: возвращаем 0
        }
    }

    // Метод для получения запросов события с Fallback
    private List<ParticipationRequestDto> getRequestsForEvent(Long eventId) {
        try {
            List<ParticipationRequestDto> requests = requestClient.getRequestsByEvent(eventId);
            return requests != null ? requests : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Не удалось получить запросы для события {}: {}, возвращаем пустой список",
                    eventId, e.getMessage());
            return Collections.emptyList();
        }
    }

    // Метод для получения списка пользователей с Fallback
    private Map<Long, UserShortDto> getUsers(List<Event> events) {
        Map<Long, UserShortDto> result = new HashMap<>();
        for (Event event : events) {
            Long userId = event.getInitiatorId();
            try {
                UserShortDto user = userClient.getUserShort(userId);
                if (user != null) {
                    result.put(userId, user);
                } else {
                    result.put(userId, createDefaultUser(userId));
                }
            } catch (Exception e) {
                log.warn("Не удалось получить пользователя {}: {}", userId, e.getMessage());
                result.put(userId, createDefaultUser(userId));
            }
        }
        return result;
    }

    private UserShortDto createDefaultUser(Long userId) {
        UserShortDto defaultUser = new UserShortDto();
        defaultUser.setId(userId);
        defaultUser.setName("Unknown User");
        return defaultUser;
    }

    // Метод для получения количества запросов для списка событий с Fallback
    private Map<Long, Long> getConfirmedRequestsCount(List<Event> events) {
        Map<Long, Long> result = new HashMap<>();
        for (Event event : events) {
            try {
                Long count = requestClient.getConfirmedRequestsCount(event.getId());
                result.put(event.getId(), count != null ? count : 0L);
            } catch (Exception e) {
                log.warn("Не удалось получить количество запросов для события {}: {}, возвращаем 0",
                        event.getId(), e.getMessage());
                result.put(event.getId(), 0L);
            }
        }
        return result;
    }

    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId).orElse(null);
    }

    private Map<Long, Category> getCategories(List<Event> events) {
        List<Long> categoryIds = events.stream()
                .map(Event::getCategoryId)
                .distinct()
                .collect(Collectors.toList());

        return categoryRepository.findAllById(categoryIds)
                .stream()
                .collect(Collectors.toMap(Category::getId, c -> c));
    }

    private Long getViewsCount(Event event) {
        return getViewsCount(List.of(event)).getOrDefault(event.getId(), 0L);
    }

    private Map<Long, Long> getViewsCount(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyMap();

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        LocalDateTime earliestCreated = events.stream()
                .map(Event::getCreatedOn)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusDays(1));

        StatRequestParamDto statRequestParamDto = new StatRequestParamDto(
                earliestCreated.minusMinutes(1).format(Constants.FORMATTER),
                LocalDateTime.now().plusMinutes(1).format(Constants.FORMATTER),
                uris,
                true
        );

        try {
            List<StatResponseDto> stats = statClient.getStats(statRequestParamDto);
            Map<Long, Long> result = new HashMap<>();
            for (StatResponseDto dto : stats) {
                Long eventId = Long.valueOf(dto.getUri().replace("/events/", ""));
                result.put(eventId, dto.getHits());
            }
            return result;
        } catch (Exception e) {
            log.warn("Не удалось получить статистику просмотров: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}