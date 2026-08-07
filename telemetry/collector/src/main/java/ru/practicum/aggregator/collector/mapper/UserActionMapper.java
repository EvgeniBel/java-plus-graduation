package ru.practicum.aggregator.collector.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.telemetry.messages.ActionType;
import ru.practicum.telemetry.messages.UserAction;

import java.time.Instant;

@Component
public class UserActionMapper {

    public UserActionAvro toAvro(UserAction proto) {
        return UserActionAvro.newBuilder()
                .setUserId(proto.getUserId())
                .setEventId(proto.getEventId())
                .setActionType(toAvroActionType(proto.getActionType()))
                .setTimestamp(Instant.parse(proto.getTimestamp()).toEpochMilli())
                .build();
    }

    private ActionTypeAvro toAvroActionType(ActionType protoType) {
        return switch (protoType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> ActionTypeAvro.VIEW;
        };
    }
}