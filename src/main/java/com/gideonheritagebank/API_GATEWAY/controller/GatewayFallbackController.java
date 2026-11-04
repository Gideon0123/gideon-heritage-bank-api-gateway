package com.gideonheritagebank.API_GATEWAY.controller;

import com.gideonheritagebank.API_GATEWAY.error.GatewayErrorResponseWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class GatewayFallbackController {

    private static final Logger log =
            LoggerFactory.getLogger(GatewayFallbackController.class);

    private final GatewayErrorResponseWriter errorResponseWriter;

    public GatewayFallbackController(
            GatewayErrorResponseWriter errorResponseWriter
    ) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @RequestMapping("/gateway")
    public Mono<Void> gatewayFallback(ServerWebExchange exchange) {

        Throwable cause = exchange.getAttribute(
                ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR
        );

        String path = exchange.getRequest()
                .getURI()
                .getRawPath();

        if (cause != null) {

            log.error(
                    "Gateway fallback invoked. path={}, exceptionType={}, message={}",
                    path,
                    cause.getClass().getName(),
                    cause.getMessage(),
                    cause
            );

        } else {

            log.error(
                    "Gateway fallback invoked without a circuit-breaker exception attribute. path={}",
                    path
            );
        }

        return errorResponseWriter.write(
                exchange,
                HttpStatus.SERVICE_UNAVAILABLE,
                "DOWNSTREAM_SERVICE_UNAVAILABLE",
                "The requested banking service is temporarily unavailable. Please try again later."
        );
    }
}