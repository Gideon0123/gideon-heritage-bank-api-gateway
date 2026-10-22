package com.gideonheritagebank.API_GATEWAY.error;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class JsonAccessDeniedHandler
        implements ServerAccessDeniedHandler {

    private final GatewayErrorResponseWriter writer;

    public JsonAccessDeniedHandler(
            GatewayErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public Mono<Void> handle(
            ServerWebExchange exchange,
            AccessDeniedException exception) {

        return writer.write(
                exchange,
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "You do not have permission to perform this operation."
        );
    }
}

