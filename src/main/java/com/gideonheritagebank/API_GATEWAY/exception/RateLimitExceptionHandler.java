package com.gideonheritagebank.API_GATEWAY.exception;

import com.gideonheritagebank.API_GATEWAY.error.GatewayErrorResponseWriter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class RateLimitExceptionHandler implements WebExceptionHandler {

    private final GatewayErrorResponseWriter errorWriter;

    public RateLimitExceptionHandler(
            GatewayErrorResponseWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public Mono<Void> handle(
            ServerWebExchange exchange,
            Throwable exception) {

        if (exception instanceof HttpClientErrorException httpException
                && httpException.getStatusCode().value() == 429) {

            return errorWriter.write(
                    exchange,
                    HttpStatus.TOO_MANY_REQUESTS,
                    "RATE_LIMIT_EXCEEDED",
                    "Too many requests. Please wait before trying again."
            );
        }

        return Mono.error(exception);
    }
}
