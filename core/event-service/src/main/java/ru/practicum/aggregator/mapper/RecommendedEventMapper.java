package ru.practicum.aggregator.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.dto.event.RecommendedEventDto;
import ru.practicum.stats.service.dashboard.RecommendationsProto;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RecommendedEventMapper {

    public RecommendedEventDto toDto(RecommendationsProto.RecommendedEventProto proto) {
        return RecommendedEventDto.builder()
                .eventId(proto.getEventId())
                .score(proto.getScore())
                .build();
    }

    public List<RecommendedEventDto> toDtoList(List<RecommendationsProto.RecommendedEventProto> protoList) {
        return protoList.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
