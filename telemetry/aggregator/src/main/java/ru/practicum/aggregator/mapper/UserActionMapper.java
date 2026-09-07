package ru.practicum.aggregator.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.aggregator.model.UserActionEvent;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.stats.service.collector.UserActionOuterClass;

import java.time.Instant;

@Slf4j
@Component
public class UserActionMapper {

    /**
     * Конвертация из Avro в внутреннюю модель
     */
    public UserActionEvent toModel(UserActionAvro avro) {
        if (avro == null) {
            log.warn("Получен null UserActionAvro");
            return null;
        }

        return UserActionEvent.builder()
                .userId(avro.getUserId())
                .eventId(avro.getEventId())
                .actionType(avro.getActionType())
                .timestamp(avro.getTimestamp())
                .build();
    }

    /**
     * Конвертация из Protobuf в внутреннюю модель
     */
    public UserActionEvent toModel(UserActionOuterClass.UserActionProto proto) {
        if (proto == null) {
            log.warn("Получен null UserActionProto");
            return null;
        }

        ActionTypeAvro actionType = convertActionType(proto.getActionType());
        long timestamp = proto.getTimestamp().getSeconds() * 1000 +
                proto.getTimestamp().getNanos() / 1_000_000;

        return UserActionEvent.builder()
                .userId(proto.getUserId())
                .eventId(proto.getEventId())
                .actionType(actionType)
                .timestamp(timestamp)
                .build();
    }

    /**
     * Конвертация из внутренней модели в Avro
     */
    public UserActionAvro toAvro(UserActionEvent event) {
        if (event == null) {
            return null;
        }

        return UserActionAvro.newBuilder()
                .setUserId(event.getUserId())
                .setEventId(event.getEventId())
                .setActionType(event.getActionType())
                .setTimestamp(event.getTimestamp())
                .build();
    }

    /**
     * Конвертация из внутренней модели в Protobuf
     */
    public UserActionOuterClass.UserActionProto toProto(UserActionEvent event) {
        if (event == null) {
            return null;
        }

        UserActionOuterClass.ActionTypeProto actionType = convertActionType(event.getActionType());
        Instant instant = Instant.ofEpochMilli(event.getTimestamp());

        return UserActionOuterClass.UserActionProto.newBuilder()
                .setUserId(event.getUserId())
                .setEventId(event.getEventId())
                .setActionType(actionType)
                .setTimestamp(
                        com.google.protobuf.Timestamp.newBuilder()
                                .setSeconds(instant.getEpochSecond())
                                .setNanos(instant.getNano())
                                .build()
                )
                .build();
    }

    /**
     * Конвертация ActionType между Avro и Protobuf
     */
    private ActionTypeAvro convertActionType(UserActionOuterClass.ActionTypeProto proto) {
        return switch (proto) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case UNRECOGNIZED -> {
                log.warn("Неизвестный тип действия: {}", proto);
                yield ActionTypeAvro.VIEW; // default
            }
        };
    }

    private UserActionOuterClass.ActionTypeProto convertActionType(ActionTypeAvro avro) {
        return switch (avro) {
            case VIEW -> UserActionOuterClass.ActionTypeProto.ACTION_VIEW;
            case REGISTER -> UserActionOuterClass.ActionTypeProto.ACTION_REGISTER;
            case LIKE -> UserActionOuterClass.ActionTypeProto.ACTION_LIKE;
        };
    }
}