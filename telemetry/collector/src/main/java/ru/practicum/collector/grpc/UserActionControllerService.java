package ru.practicum.collector.grpc;

import ru.practicum.ewm.stats.proto.Empty;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.collector.kafka.KafkaProducerService;
import ru.practicum.collector.mapper.UserActionMapper;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionControllerService extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final KafkaProducerService kafkaProducerService;
    private final UserActionMapper userActionMapper;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Получено действие пользователя: userId={}, eventId={}, actionType={}, timestamp={}",
                    request.getUserId(),
                    request.getEventId(),
                    request.getActionType(),
                    request.hasTimestamp() ? request.getTimestamp() : null);

            validateRequest(request);

            var avroAction = userActionMapper.toAvro(request);

            kafkaProducerService.sendUserAction(avroAction);

            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            log.error("Ошибка валидации: {}", e.getMessage());
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Ошибка обработки действия пользователя: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Внутренняя ошибка сервера")
                    .asRuntimeException());
        }
    }

    private void validateRequest(UserActionProto request) {
        if (request.getUserId() <= 0) {
            throw new IllegalArgumentException("userId должен быть положительным числом");
        }
        if (request.getEventId() <= 0) {
            throw new IllegalArgumentException("eventId должен быть положительным числом");
        }
        if (!request.hasTimestamp()) {
            throw new IllegalArgumentException("timestamp обязателен");
        }
        // Проверка типа действия - теперь ACTION_VIEW = 0
        if (request.getActionType() == ActionTypeProto.ACTION_VIEW) {
            // OK
        } else if (request.getActionType() == ActionTypeProto.ACTION_REGISTER) {
            // OK
        } else if (request.getActionType() == ActionTypeProto.ACTION_LIKE) {

        } else {
            throw new IllegalArgumentException("Некорректный тип действия");
        }
    }
}