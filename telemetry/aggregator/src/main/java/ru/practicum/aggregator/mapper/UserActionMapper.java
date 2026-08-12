package ru.practicum.aggregator.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.aggregator.model.UserActionEvent;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
public class UserActionMapper {

    public UserActionEvent toModel(UserActionAvro avro) {
        return UserActionEvent.builder()
                .userId(avro.getUserId())
                .eventId(avro.getEventId())
                .actionType(avro.getActionType())
                .timestamp(avro.getTimestamp())
                .build();
    }
}
