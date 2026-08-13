package ru.practicum.analyzer;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;

@Slf4j
@GrpcAdvice
public class GlobalExceptionHandler {

    @GrpcExceptionHandler(IllegalArgumentException.class)
    public StatusRuntimeException handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Invalid argument: {}", e.getMessage());
        return Status.INVALID_ARGUMENT
                .withDescription(e.getMessage())
                .asRuntimeException();
    }

    @GrpcExceptionHandler(Throwable.class)
    public StatusRuntimeException handleGenericException(Throwable e) {
        log.error("Internal error: {}", e.getMessage(), e);
        return Status.INTERNAL
                .withDescription("Internal server error: " + e.getMessage())
                .asRuntimeException();
    }
}
