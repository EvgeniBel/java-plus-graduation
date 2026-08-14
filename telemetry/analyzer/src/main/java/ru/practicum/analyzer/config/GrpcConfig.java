package ru.practicum.analyzer.config;

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

            String methodName = call.getMethodDescriptor().getFullMethodName();
            log.info("gRPC запрос: {}", methodName);

            long startTime = System.currentTimeMillis();
            ServerCall.Listener<ReqT> listener = next.startCall(call, headers);

            return new ServerCall.Listener<ReqT>() {
                @Override
                public void onMessage(ReqT message) {
                    log.debug("Получено сообщение для {}: {}", methodName, message);
                    listener.onMessage(message);
                }

                @Override
                public void onComplete() {
                    log.debug("Запрос завершен: {}, время: {}ms",
                            methodName, System.currentTimeMillis() - startTime);
                    listener.onComplete();
                }

                @Override
                public void onCancel() {
                    log.debug("Запрос отменен: {}", methodName);
                    listener.onCancel();
                }

                @Override
                public void onReady() {
                    listener.onReady();
                }

                @Override
                public void onHalfClose() {
                    listener.onHalfClose();
                }
            };
        }
    }
}