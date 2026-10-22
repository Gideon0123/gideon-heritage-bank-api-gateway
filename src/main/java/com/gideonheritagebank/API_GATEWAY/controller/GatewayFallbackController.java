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

    private final GatewayErrorResponseWriter errorResponseWriter;

    public GatewayFallbackController(
            GatewayErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @RequestMapping("/gateway")
    public Mono<Void> gatewayFallback(ServerWebExchange exchange) {
        return errorResponseWriter.write(
                exchange,
                HttpStatus.SERVICE_UNAVAILABLE,
                "DOWNSTREAM_SERVICE_UNAVAILABLE",
                "The requested banking service is temporarily unavailable. Please try again later."
        );
    }
}