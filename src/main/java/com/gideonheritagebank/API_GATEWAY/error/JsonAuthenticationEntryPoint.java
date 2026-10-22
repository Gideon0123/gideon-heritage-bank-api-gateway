package com.gideonheritagebank.API_GATEWAY.error;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class JsonAuthenticationEntryPoint
        implements ServerAuthenticationEntryPoint {

    private final GatewayErrorResponseWriter writer;

    public JsonAuthenticationEntryPoint(
            GatewayErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public Mono<Void> commence(
            ServerWebExchange exchange,
            AuthenticationException exception) {

        return writer.write(
                exchange,
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED",
                "Authentication is required or the access token is invalid."
        );
    }
}
