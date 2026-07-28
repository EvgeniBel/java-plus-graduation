package ru.practicum.dto.event;

import lombok.*;
import ru.practicum.dto.request.ParticipationRequestDto;


import java.util.List;

@Value
@Builder
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class EventRequestStatusUpdateResult {
    List<ParticipationRequestDto> confirmedRequests;
    List<ParticipationRequestDto> rejectedRequests;
}