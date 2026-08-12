package ru.practicum.aggregator.service;

import ru.practicum.dto.event.*;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

public interface EventService {

    EventFullDto addEvent(Long userId, NewEventDto dto);

    List<EventShortDto> getEventsOfUser(Long userId, Integer from, Integer size);

    EventFullDto getEventById(Long userId, Long eventId);

    List<EventShortDto> getShortEventsInfoByIds(List<Long> eventIds);

    EventFullDto patchEventById(Long userId, Long eventId, UpdateEventUserRequest dto);

    List<ParticipationRequestDto> getRequestsOfEvent(Long userId, Long eventId);

    EventRequestStatusUpdateResult patchRequestsStatusOfEvent(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest dto);

    List<EventFullDto> getEventsByAdminRequest(AdminEventRequestParam param);

    EventFullDto patchEventByIdByAdmin(Long eventId, UpdateEventAdminRequest dto);

    List<EventShortDto> getEventsByPublicRequest(PublicEventRequestParam param);

    EventFullDto getEventByIdByPublicRequest(Long eventId, Long userId);

    boolean eventExists(Long eventId);

    EventFullDto getEventFull(Long eventId);

    EventShortDto getEventShort(Long eventId);

    String getEventStatus(Long eventId);

    // ===== МЕТОДЫ ДЛЯ РЕЙТИНГА =====

    double getEventRating(Long eventId);

    Map<Long, Double> getEventsRatings(List<Long> eventIds);

    void sendViewAction(Long userId, Long eventId);

    boolean hasUserVisitedEvent(Long userId, Long eventId);
}