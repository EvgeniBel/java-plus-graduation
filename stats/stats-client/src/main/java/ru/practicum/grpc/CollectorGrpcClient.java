package ru.practicum.grpc;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.stats.service.collector.UserActionControllerGrpc;
import ru.practicum.stats.service.collector.UserActionOuterClass.ActionTypeProto;
import ru.practicum.stats.service.collector.UserActionOuterClass.UserActionProto;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorGrpcClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;

    public boolean sendUserAction(Long userId, Long eventId, ActionTypeProto actionType, Timestamp timestamp) {
        try {
            if (userId == null || userId <= 0) {
                log.warn("Невалидный userId: {}", userId);
                return false;
            }
            if (eventId == null || eventId <= 0) {
                log.warn("Невалидный eventId: {}", eventId);
                return false;
            }
            if (actionType == null) {
                log.warn("actionType не может быть null");
                return false;
            }

            UserActionProto request = UserActionProto.newBuilder()
                    .setUserId(userId)
                    .setEventId(eventId)
                    .setActionType(actionType)
                    .setTimestamp(timestamp != null ? timestamp : createTimestamp())
                    .build();

            log.info("Отправка действия в Collector: userId={}, eventId={}, actionType={}",
                    userId, eventId, actionType);

            Empty response = collectorStub.collectUserAction(request);

            log.info("Действие успешно отправлено");
            return true;

        } catch (StatusRuntimeException e) {
            log.error("Ошибка gRPC вызова Collector: status={}, message={}",
                    e.getStatus().getCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Ошибка вызова Collector: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean sendUserAction(Long userId, Long eventId, ActionTypeProto actionType) {
        return sendUserAction(userId, eventId, actionType, createTimestamp());
    }

    public boolean sendViewAction(Long userId, Long eventId) {
        return sendUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW);
    }

    public boolean sendLikeAction(Long userId, Long eventId) {
        return sendUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }

    public boolean sendRegisterAction(Long userId, Long eventId) {
        return sendUserAction(userId, eventId, ActionTypeProto.ACTION_REGISTER);
    }

    private Timestamp createTimestamp() {
        Instant now = Instant.now();
        return Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();
    }
}