package ru.practicum.collector.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.collector.kafka.KafkaProducerService;
import ru.practicum.collector.mapper.UserActionMapper;
import ru.practicum.telemetry.common.OperationStatus;
import ru.practicum.telemetry.messages.UserAction;
import ru.practicum.telemetry.messages.UserActionRequest;
import ru.practicum.telemetry.services.CollectorControllerGrpc;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class CollectorGrpcService extends CollectorControllerGrpc.CollectorControllerImplBase {

    private final KafkaProducerService kafkaProducerService;
    private final UserActionMapper userActionMapper;

    @Override
    public void sendUserAction(UserActionRequest request, StreamObserver<OperationStatus> responseObserver) {
        try {
            UserAction action = request.getAction();

            log.info("Получено действие пользователя: userId={}, eventId={}, actionType={}, timestamp={}",
                    action.getUserId(),
                    action.getEventId(),
                    action.getActionType(),
                    action.getTimestamp());

            // Проверка валидности
            if (action.getUserId() <= 0) {
                log.error("Некорректный userId: {}", action.getUserId());
                responseObserver.onNext(OperationStatus.newBuilder()
                        .setSuccess(false)
                        .setMessage("userId должен быть положительным")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            if (action.getEventId() <= 0) {
                log.error("Некорректный eventId: {}", action.getEventId());
                responseObserver.onNext(OperationStatus.newBuilder()
                        .setSuccess(false)
                        .setMessage("eventId должен быть положительным")
                        .build());
                responseObserver.onCompleted();
                return;
            }

            // Конвертация gRPC → Avro
            var avroAction = userActionMapper.toAvro(action);

            // Отправка в Kafka
            kafkaProducerService.sendUserAction(avroAction);

            // Успешный ответ
            responseObserver.onNext(OperationStatus.newBuilder()
                    .setSuccess(true)
                    .setMessage("Действие успешно отправлено")
                    .build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Ошибка обработки действия пользователя: {}", e.getMessage(), e);
            responseObserver.onNext(OperationStatus.newBuilder()
                    .setSuccess(false)
                    .setMessage(String.format("Ошибка: %s",e.getMessage()))
                    .build());
            responseObserver.onCompleted();
        }
    }
}