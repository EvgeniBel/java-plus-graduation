package ru.practicum.aggregator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActionEvent {
    private Long userId;
    private Long eventId;
    private ActionTypeAvro actionType;
    private Long timestamp;
}
