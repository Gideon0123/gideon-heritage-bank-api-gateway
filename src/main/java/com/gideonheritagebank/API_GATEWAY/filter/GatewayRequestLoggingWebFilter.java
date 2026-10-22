package com.gideonheritagebank.API_GATEWAY.filter;

import java.util.UUID;

import com.gideonheritagebank.API_GATEWAY.error.GatewayErrorResponseWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayRequestLoggingWebFilter implements WebFilter {

    private static final Logger log =
            LoggerFactory.getLogger(GatewayRequestLoggingWebFilter.class);

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain) {

        String requestId = UUID.randomUUID().toString();

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> headers.set(
                        REQUEST_ID_HEADER,
                        requestId
                ))
                .build();

        ServerWebExchange updatedExchange = exchange.mutate()
                .request(request)
                .build();

        updatedExchange.getAttributes().put(
                GatewayErrorResponseWriter.REQUEST_ID_ATTRIBUTE,
                requestId
        );

        updatedExchange.getResponse().getHeaders().set(
                REQUEST_ID_HEADER,
                requestId
        );

        long startNanos = System.nanoTime();

        return chain.filter(updatedExchange)
                .doFinally(signal -> {
                    long durationMs =
                            (System.nanoTime() - startNanos) / 1_000_000;

                    Integer status = updatedExchange.getResponse()
                            .getStatusCode() != null
                            ? updatedExchange.getResponse()
                            .getStatusCode().value()
                            : null;

                    log.info(
                            "gateway_request requestId={} method={} path={} status={} durationMs={} outcome={}",
                            requestId,
                            request.getMethod(),
                            request.getURI().getRawPath(),
                            status,
                            durationMs,
                            signal
                    );
                });
    }
}
