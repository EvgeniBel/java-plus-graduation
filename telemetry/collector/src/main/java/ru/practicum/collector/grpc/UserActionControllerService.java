package ru.practicum.collector.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.collector.kafka.KafkaProducerService;
import ru.practicum.collector.mapper.UserActionMapper;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.Empty;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.telemetry.messages.ActionType;
import ru.practicum.telemetry.messages.UserAction;

import java.time.Instant;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionControllerService extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final KafkaProducerService kafkaProducerService;
    private final UserActionMapper userActionMapper;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Получено действие через UserActionController: userId={}, eventId={}, actionType={}, timestamp={}",
                    request.getUserId(),
                    request.getEventId(),
                    request.getActionType(),
                    request.getTimestamp());

            UserAction action = convertToTelemetryAction(request);
            var avroAction = userActionMapper.toAvro(action);
            kafkaProducerService.sendUserAction(avroAction);

            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Ошибка обработки действия: {}", e.getMessage(), e);
            responseObserver.onError(e);
        }
    }

    private UserAction convertToTelemetryAction(UserActionProto proto) {
        return UserAction.newBuilder()
                .setUserId(proto.getUserId())
                .setEventId(proto.getEventId())
                .setActionType(convertActionType(proto.getActionType()))
                .setTimestamp(proto.getTimestamp())
                .build();
    }

    private ActionType convertActionType(ActionTypeProto protoType) {
        return switch (protoType) {
            case ACTION_VIEW -> ActionType.ACTION_VIEW;
            case ACTION_REGISTER -> ActionType.ACTION_REGISTER;
            case ACTION_LIKE -> ActionType.ACTION_LIKE;
            default -> ActionType.ACTION_UNKNOWN;
        };
    }
}