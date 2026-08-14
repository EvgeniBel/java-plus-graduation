package ru.practicum.grpc;

import io.grpc.Metadata;
import io.grpc.ClientInterceptor;
import io.grpc.ClientCall;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Status;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

    @GrpcGlobalClientInterceptor
    public ClientInterceptor loggingClientInterceptor() {
        return new LoggingClientInterceptor();
    }

    private static class LoggingClientInterceptor implements ClientInterceptor {
        private static final org.slf4j.Logger log =
                org.slf4j.LoggerFactory.getLogger(LoggingClientInterceptor.class);

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                io.grpc.MethodDescriptor<ReqT, RespT> method,
                io.grpc.CallOptions callOptions,
                io.grpc.Channel next) {

            String methodName = method.getFullMethodName();
            log.debug("gRPC клиент запрос: {}", methodName);

            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                    next.newCall(method, callOptions)) {

                private long startTime;

                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    startTime = System.currentTimeMillis();
                    log.debug("Начало вызова: {}", methodName);

                    super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(
                            responseListener) {
                        @Override
                        public void onClose(Status status, Metadata trailers) {
                            long duration = System.currentTimeMillis() - startTime;
                            if (status.isOk()) {
                                log.debug("Успешный вызов: {}, время: {}ms", methodName, duration);
                            } else {
                                log.error("Ошибка вызова: {}, статус: {}, время: {}ms",
                                        methodName, status, duration);
                            }
                            super.onClose(status, trailers);
                        }
                    }, headers);
                }
            };
        }
    }
}