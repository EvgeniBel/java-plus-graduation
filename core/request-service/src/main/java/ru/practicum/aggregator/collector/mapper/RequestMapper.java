package ru.practicum.aggregator.collector.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.constants.Constants;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.aggregator.model.ParticipationRequest;
import ru.practicum.dto.request.RequestStatus;

import java.time.LocalDateTime;

@UtilityClass
public class RequestMapper {

    public ParticipationRequest toEntity(
            LocalDateTime created,
            Long eventId,
            Long requesterId,
            RequestStatus status
    ) {
        return ParticipationRequest.builder()
                .created(created)
                .eventId(eventId)
                .requesterId(requesterId)
                .status(status)
                .build();
    }

    public ParticipationRequestDto toParticipationRequestDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated().format(Constants.FORMATTER))
                .event(request.getEventId())
                .requester(request.getRequesterId())
                .status(request.getStatus().toString())
                .build();
    }
}