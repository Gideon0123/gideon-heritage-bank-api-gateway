package com.gideonheritagebank.API_GATEWAY.exception;

import com.gideonheritagebank.API_GATEWAY.error.GatewayErrorResponseWriter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;

import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;

@Component
@Order(-2)
public class GatewayNotFoundExceptionHandler
        implements WebExceptionHandler {

    private final GatewayErrorResponseWriter errorResponseWriter;

    public GatewayNotFoundExceptionHandler(
            GatewayErrorResponseWriter errorResponseWriter) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public Mono<Void> handle(
            ServerWebExchange exchange,
            Throwable exception) {

        // Do not replace errors produced by a matched downstream route.
        boolean noMatchedRoute =
                exchange.getAttribute(
                        ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR
                ) == null;

        if (!noMatchedRoute || exchange.getResponse().isCommitted()) {
            return Mono.error(exception);
        }

        if (isNotFound(exception)) {
            return errorResponseWriter.write(
                    exchange,
                    HttpStatus.NOT_FOUND,
                    "GATEWAY_ROUTE_NOT_FOUND",
                    "No gateway route matches the requested resource."
            );
        }

        // Let the normal error handling process other errors.
        return Mono.error(exception);
    }

    private boolean isNotFound(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof ErrorResponse errorResponse
                    && errorResponse.getStatusCode().value() == 404) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}
