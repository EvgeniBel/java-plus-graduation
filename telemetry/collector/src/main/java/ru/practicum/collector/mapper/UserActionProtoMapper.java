package ru.practicum.collector.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.stats.service.collector.UserActionOuterClass;

import java.time.Instant;

@Component
public class UserActionProtoMapper {

    /**
     * Конвертация UserActionProto -> UserActionAvro
     */
    public UserActionAvro mapFromProto(UserActionOuterClass.UserActionProto proto) {
        // Валидация
        validateUserAction(proto);

        // Маппинг ActionTypeProto -> ActionTypeAvro
        ActionTypeAvro actionTypeAvro = mapActionType(proto.getActionType());

        // Конвертация google.protobuf.Timestamp в миллисекунды
        long timestampMillis = convertTimestampToMillis(proto.getTimestamp());

        return UserActionAvro.newBuilder()
                .setUserId(proto.getUserId())
                .setEventId(proto.getEventId())
                .setActionType(actionTypeAvro)
                .setTimestamp(timestampMillis)
                .build();
    }

    /**
     * Валидация входных данных
     */
    private void validateUserAction(UserActionOuterClass.UserActionProto request) {
        if (request.getUserId() <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        if (request.getEventId() <= 0) {
            throw new IllegalArgumentException("Event ID must be positive");
        }
        if (request.getActionType() == UserActionOuterClass.ActionTypeProto.UNRECOGNIZED) {
            throw new IllegalArgumentException("Unknown action type: " + request.getActionType());
        }
        if (!request.hasTimestamp()) {
            throw new IllegalArgumentException("Timestamp is required");
        }
    }

    /**
     * Маппинг типов действий из Proto в Avro
     */
    private ActionTypeAvro mapActionType(UserActionOuterClass.ActionTypeProto protoType) {
        return switch (protoType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> throw new IllegalArgumentException("Unknown action type: " + protoType);
        };
    }

    /**
     * Конвертация google.protobuf.Timestamp в миллисекунды
     */
    private long convertTimestampToMillis(com.google.protobuf.Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos())
                .toEpochMilli();
    }
}
