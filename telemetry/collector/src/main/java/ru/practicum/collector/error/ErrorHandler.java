package ru.practicum.collector.error;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;

@Slf4j
@GrpcAdvice
public class ErrorHandler {

    @GrpcExceptionHandler(IllegalArgumentException.class)
    public StatusRuntimeException handleIllegalArgument(IllegalArgumentException e) {
        log.error("Ошибка валидации: {}", e.getMessage());
        return Status.INVALID_ARGUMENT
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(Exception.class)
    public StatusRuntimeException handleException(Exception e) {
        log.error("Внутренняя ошибка: {}", e.getMessage(), e);
        return Status.INTERNAL
                .withDescription("Внутренняя ошибка сервера")
                .asRuntimeException();
    }
}