package ru.practicum.collector.config;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {

    @GrpcGlobalServerInterceptor
    public ServerInterceptor loggingInterceptor() {
        return new LoggingInterceptor();
    }

    private static class LoggingInterceptor implements ServerInterceptor {
        private static final org.slf4j.Logger log =
                org.slf4j.LoggerFactory.getLogger(LoggingInterceptor.class);

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                ServerCall<ReqT, RespT> call,
                Metadata headers,
                ServerCallHandler<ReqT, RespT> next) {

            log.debug("gRPC запрос: {}", call.getMethodDescriptor().getFullMethodName());
            return next.startCall(call, headers);
        }
    }
}