package ru.practicum.aggregator.collector.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.aggregator.model.Category;
import ru.practicum.aggregator.model.Event;
import ru.practicum.aggregator.model.EventState;
import ru.practicum.aggregator.model.Location;
import ru.practicum.constants.Constants;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.dto.user.UserShortDto;

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
            Double rating
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
                .rating(rating != null ? rating : 0.0)
                .build();
    }

    public EventShortDto eventToShortDto(
            Event event,
            UserShortDto initiator,
            Category category,
            Long confirmedRequests,
            Double rating
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
                .rating(rating != null ? rating : 0.0)
                .state(event.getState().toString())
                .build();
    }
}