package ru.practicum.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.constants.Constants;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.model.Category;
import ru.practicum.model.Event;
import ru.practicum.model.EventState;
import ru.practicum.model.Location;

import java.time.LocalDateTime;

@UtilityClass
public class EventMapper {

    public Event dtoToEvent(
            NewEventDto eventDto,
            Long categoryId,
            LocalDateTime createdOn,
            Long initiatorId,
            Location location,
            LocalDateTime publishedOn,
            EventState state
    ) {
        return Event.builder()
                .annotation(eventDto.getAnnotation())
                .categoryId(categoryId)
                .createdOn(createdOn)
                .description(eventDto.getDescription())
                .eventDate(LocalDateTime.parse(eventDto.getEventDate(), Constants.FORMATTER))
                .initiatorId(initiatorId)
                .location(location)
                .paid(eventDto.getPaid() != null ? eventDto.getPaid() : false)
                .participantLimit(eventDto.getParticipantLimit() != null ? eventDto.getParticipantLimit() : 0)
                .publishedOn(publishedOn)
                .requestModeration(eventDto.getRequestModeration() != null ? eventDto.getRequestModeration() : true)
                .state(state)
                .title(eventDto.getTitle())
                .build();
    }

    public EventFullDto eventToFullDto(
            Event event,
            UserShortDto initiator,
            Category category,
            Long confirmedRequests,
            Long views
    ) {
        return EventFullDto.builder()
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toCategoryDto(category))
                .confirmedRequests(confirmedRequests != null ? confirmedRequests : 0L)
                .createdOn(event.getCreatedOn().format(Constants.FORMATTER))
                .description(event.getDescription())
                .eventDate(event.getEventDate().format(Constants.FORMATTER))
                .id(event.getId())
                .initiator(initiator)
                .location(LocationMapper.locationToDto(event.getLocation()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn() != null ? event
                        .getPublishedOn().format(Constants.FORMATTER) : null)
                .requestModeration(event.getRequestModeration())
                .state(event.getState().toString())
                .title(event.getTitle())
                .views(views != null ? views : 0L)
                .build();
    }

    public EventShortDto eventToShortDto(
            Event event,
            UserShortDto initiator,
            Category category,
            Long confirmedRequests,
            Long views
    ) {
        return EventShortDto.builder()
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toCategoryDto(category))
                .confirmedRequests(confirmedRequests != null ? confirmedRequests : 0L)
                .eventDate(event.getEventDate().format(Constants.FORMATTER))
                .id(event.getId())
                .initiator(initiator)
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(views != null ? views : 0L)
                .state(event.getState().toString())
                .build();
    }
}