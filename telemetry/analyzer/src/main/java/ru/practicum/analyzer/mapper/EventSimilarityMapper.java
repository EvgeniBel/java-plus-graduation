package ru.practicum.analyzer.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;


import java.time.Instant;

@UtilityClass
public class EventSimilarityMapper {

    public static EventSimilarity toEntity(EventSimilarityAvro avro) {
        return EventSimilarity.builder()
                .event1(avro.getEventA())
                .event2(avro.getEventB())
                .similarity(avro.getScore())
                .timestamp(Instant.ofEpochSecond(avro.getTimestamp()))
                .build();
    }
}
