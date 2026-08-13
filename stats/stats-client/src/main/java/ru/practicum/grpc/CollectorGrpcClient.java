package ru.practicum.grpc;


import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.stats.service.collector.UserActionControllerGrpc;
import ru.practicum.stats.service.collector.UserActionOuterClass;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorGrpcClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub stub;

    public void collectUserAction(Long userId, Long eventId, UserActionOuterClass.ActionTypeProto actionType) {
        try {
            UserActionOuterClass.UserActionProto action = UserActionOuterClass.UserActionProto.newBuilder()
                    .setUserId(userId)
                    .setEventId(eventId)
                    .setActionType(actionType)
                    .setTimestamp(Timestamp.newBuilder()
                            .setSeconds(System.currentTimeMillis() / 1000)
                            .setNanos((int) ((System.currentTimeMillis() % 1000) * 1_000_000))
                            .build())
                    .build();

            stub.collectUserAction(action);
            log.debug("Отправлено действие в Collector: userId={}, eventId={}, actionType={}",
                    userId, eventId, actionType);

        } catch (Exception e) {
            log.error("Ошибка отправки действия в Collector: userId={}, eventId={}, actionType={}",
                    userId, eventId, actionType, e);
            throw new RuntimeException("Ошибка отправки действия в Collector", e);
        }
    }

}