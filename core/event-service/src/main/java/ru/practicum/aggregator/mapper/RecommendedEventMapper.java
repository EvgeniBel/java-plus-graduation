package ru.practicum.aggregator.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.dto.event.RecommendedEventDto;
import ru.practicum.telemetry.messages.RecommendedEvent;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RecommendedEventMapper {

    public RecommendedEventDto toDto(RecommendedEvent proto) {
        return RecommendedEventDto.builder()
                .eventId(proto.getEventId())
                .score(proto.getScore())
                .build();
    }

    public List<RecommendedEventDto> toDtoList(List<RecommendedEvent> protoList) {
        return protoList.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
