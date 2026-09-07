package ru.practicum.collector.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.stats.service.collector.UserActionOuterClass;

@Component
@Slf4j
public class UserActionMapper {

    public UserActionAvro toAvro(UserActionOuterClass.UserActionProto proto) {
        long timestampMillis = proto.hasTimestamp()
                ? proto.getTimestamp().getSeconds() * 1000 + proto.getTimestamp().getNanos() / 1_000_000
                : System.currentTimeMillis();

        return UserActionAvro.newBuilder()
                .setUserId(proto.getUserId())
                .setEventId(proto.getEventId())
                .setActionType(toAvroActionType(proto.getActionType()))
                .setTimestamp(timestampMillis)
                .build();
    }

    private ActionTypeAvro toAvroActionType(UserActionOuterClass.ActionTypeProto protoType) {
        return switch (protoType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> {
                log.warn("Неизвестный тип действия: {}, используется VIEW по умолчанию", protoType);
                yield ActionTypeAvro.VIEW;
            }
        };
    }
}