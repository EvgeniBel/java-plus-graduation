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

    // метод для валидации
    public boolean isValid() {
        return userId != null && userId > 0
                && eventId != null && eventId > 0
                && actionType != null
                && timestamp != null && timestamp > 0;
    }
}
