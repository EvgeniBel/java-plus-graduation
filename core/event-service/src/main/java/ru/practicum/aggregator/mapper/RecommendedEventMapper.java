package ru.practicum.aggregator.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.dto.event.RecommendedEventDto;
import ru.practicum.stats.service.dashboard.RecommendationsProto;


import java.util.List;
import java.util.stream.Collectors;

@Component
public class RecommendedEventMapper {

    // Для работы с DTO из grpc клиента
    public RecommendedEventDto toDto(ru.practicum.grpc.RecommendedEvent event) {
        return RecommendedEventDto.builder()
                .eventId(event.getEventId())
                .score(event.getScore())
                .build();
    }

    // Для работы с proto (если нужно)
    public RecommendedEventDto toDto(RecommendationsProto.RecommendedEventProto proto) {
        return RecommendedEventDto.builder()
                .eventId(proto.getEventId())
                .score(proto.getScore())
                .build();
    }

    public List<RecommendedEventDto> toDtoList(List<ru.practicum.grpc.RecommendedEvent> events) {
        return events.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
