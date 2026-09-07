package ru.practicum.aggregator.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserActionEvent {
    Long userId;
    Long eventId;
    ActionTypeAvro actionType;
    Long timestamp;

    // метод для валидации
    public boolean isValid() {
        return userId != null && userId > 0
                && eventId != null && eventId > 0
                && actionType != null
                && timestamp != null && timestamp > 0;
    }
}
