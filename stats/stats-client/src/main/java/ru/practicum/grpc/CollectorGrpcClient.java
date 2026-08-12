package ru.practicum.grpc;

import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.Empty;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorGrpcClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;

    public boolean sendUserAction(Long userId, Long eventId, ActionTypeProto actionType, Timestamp timestamp) {
        try {
            UserActionProto request = UserActionProto.newBuilder()
                    .setUserId(userId)
                    .setEventId(eventId)
                    .setActionType(actionType)
                    .setTimestamp(timestamp)
                    .build();

            log.info("Отправка действия в Collector: userId={}, eventId={}, actionType={}, timestamp={}",
                    userId, eventId, actionType, timestamp);

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
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(Instant.now().getEpochSecond())
                .setNanos(Instant.now().getNano())
                .build();
        return sendUserAction(userId, eventId, actionType, timestamp);
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
}
