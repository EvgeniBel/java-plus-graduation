package ru.practicum.aggregator.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.aggregator.model.UserActionEvent;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.telemetry.messages.ActionType;

@Component
public class UserActionMapper {

    public UserActionEvent toModel(UserActionAvro avro) {
        return UserActionEvent.builder()
                .userId(avro.getUserId())
                .eventId(avro.getEventId())
                .actionType(toProtoActionType(avro.getActionType()))
                .timestamp(avro.getTimestamp())
                .build();
    }

    private ActionType toProtoActionType(ActionTypeAvro avroType) {
        return switch (avroType) {
            case VIEW -> ActionType.ACTION_VIEW;
            case REGISTER -> ActionType.ACTION_REGISTER;
            case LIKE -> ActionType.ACTION_LIKE;
            default -> ActionType.ACTION_VIEW;
        };
    }
}
