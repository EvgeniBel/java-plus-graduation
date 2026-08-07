package ru.practicum.aggregator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMaxWeight {
    private Long userId;
    private Long eventId;
    private Integer maxWeight;
    private Long timestamp;
}