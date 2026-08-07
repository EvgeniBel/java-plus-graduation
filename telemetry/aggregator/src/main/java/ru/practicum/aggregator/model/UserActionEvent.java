package ru.practicum.aggregator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.telemetry.messages.ActionType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActionEvent {
    private Long userId;
    private Long eventId;
    private ActionType actionType;
    private Long timestamp;
}
