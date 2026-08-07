package ru.practicum.aggregator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSimilarity {
    private Long eventA;
    private Long eventB;
    private Double score;
    private Long timestamp;
}