package com.gideonheritagebank.API_GATEWAY.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gideonheritagebank.API_GATEWAY.error.ApiErrorResponse;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class GatewayNotFoundWebFilter implements WebFilter {

    private final ObjectMapper objectMapper;

    public GatewayNotFoundWebFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain) {

        return chain.filter(exchange)
                .then(Mono.defer(() -> {
                    boolean noMatchedRoute =
                            exchange.getAttribute(
                                    ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR
                            ) == null;

                    boolean isNotFound =
                            exchange.getResponse().getStatusCode()
                                    == HttpStatus.NOT_FOUND;

                    boolean responseNotCommitted =
                            !exchange.getResponse().isCommitted();

                    if (!noMatchedRoute
                            || !isNotFound
                            || !responseNotCommitted) {
                        return Mono.empty();
                    }

                    return writeNotFoundResponse(exchange);
                }));
    }

    private Mono<Void> writeNotFoundResponse(
            ServerWebExchange exchange) {

        var response = exchange.getResponse();

        ApiErrorResponse error = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                "GATEWAY_ROUTE_NOT_FOUND",
                "No gateway route matches the requested resource.",
                exchange.getRequest().getPath().value(),
                exchange.getAttribute("requestId")
        );

        response.setStatusCode(HttpStatus.NOT_FOUND);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] body = objectMapper.writeValueAsBytes(error);

            return response.writeWith(Mono.just(
                    response.bufferFactory().wrap(body)
            ));
        } catch (Exception exception) {
            return Mono.error(exception);
        }
    }
}