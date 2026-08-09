package ru.practicum.grpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.telemetry.common.OperationStatus;
import ru.practicum.telemetry.messages.ActionType;
import ru.practicum.telemetry.messages.UserAction;
import ru.practicum.telemetry.messages.UserActionRequest;
import ru.practicum.telemetry.services.CollectorControllerGrpc;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorGrpcClient {

    @GrpcClient("collector-service")
    private CollectorControllerGrpc.CollectorControllerBlockingStub collectorStub;

    public boolean sendUserAction(Long userId, Long eventId, ActionType actionType, String timestamp) {
        try {
            UserAction action = UserAction.newBuilder()
                    .setUserId(userId)
                    .setEventId(eventId)
                    .setActionType(actionType)
                    .setTimestamp(timestamp)
                    .build();

            UserActionRequest request = UserActionRequest.newBuilder()
                    .setAction(action)
                    .build();

            log.info("Отправка действия в Collector: userId={}, eventId={}, actionType={}",
                    userId, eventId, actionType);

            OperationStatus response = collectorStub.sendUserAction(request);

            if (response.getSuccess()) {
                log.info("Действие успешно отправлено: {}", response.getMessage());
                return true;
            } else {
                log.error("Ошибка отправки: {}", response.getMessage());
                return false;
            }

        } catch (Exception e) {
            log.error("Ошибка вызова Collector: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean sendUserAction(Long userId, Long eventId, ActionType actionType) {
        return sendUserAction(userId, eventId, actionType, Instant.now().toString());
    }
}
