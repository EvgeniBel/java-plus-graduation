package ru.practicum.collector.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.collector.kafka.KafkaProducerService;
import ru.practicum.collector.mapper.UserActionMapper;
import ru.practicum.stats.service.collector.UserActionControllerGrpc;
import ru.practicum.stats.service.collector.UserActionOuterClass;


@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionControllerService extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final KafkaProducerService kafkaProducerService;
    private final UserActionMapper userActionMapper;

    @Override
    @Transactional
    public void collectUserAction(UserActionOuterClass.UserActionProto request,
                                  StreamObserver<com.google.protobuf.Empty> responseObserver) {
        try {
            log.info("Получено действие пользователя: userId={}, eventId={}, actionType={}, timestamp={}",
                    request.getUserId(),
                    request.getEventId(),
                    request.getActionType(),
                    request.hasTimestamp() ? request.getTimestamp() : null);

            validateRequest(request);

            var avroAction = userActionMapper.toAvro(request);

            kafkaProducerService.sendUserAction(avroAction);

            responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
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

    private void validateRequest(UserActionOuterClass.UserActionProto request) {
        if (request.getUserId() <= 0) {
            throw new IllegalArgumentException("userId должен быть положительным числом");
        }
        if (request.getEventId() <= 0) {
            throw new IllegalArgumentException("eventId должен быть положительным числом");
        }
        if (!request.hasTimestamp()) {
            throw new IllegalArgumentException("timestamp обязателен");
        }

        UserActionOuterClass.ActionTypeProto actionType = request.getActionType();
        if (actionType != UserActionOuterClass.ActionTypeProto.ACTION_VIEW &&
                actionType != UserActionOuterClass.ActionTypeProto.ACTION_REGISTER &&
                actionType != UserActionOuterClass.ActionTypeProto.ACTION_LIKE) {
            throw new IllegalArgumentException(String.format("Некорректный тип действия: %s", actionType));
        }
    }
}