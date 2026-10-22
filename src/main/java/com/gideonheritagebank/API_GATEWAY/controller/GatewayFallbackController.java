package com.gideonheritagebank.API_GATEWAY.controller;

import com.gideonheritagebank.API_GATEWAY.error.GatewayErrorResponseWriter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class GatewayFallbackController {

    private final GatewayErrorResponseWriter errorWriter;

    public GatewayFallbackController(
            GatewayErrorResponseWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @RequestMapping("/gateway")
    public Mono<Void> gatewayFallback(ServerWebExchange exchange) {
        return errorWriter.write(
                exchange,
                HttpStatus.SERVICE_UNAVAILABLE,
                "DOWNSTREAM_SERVICE_UNAVAILABLE",
                "The requested service is temporarily unavailable. Please try again later."
        );
    }
}