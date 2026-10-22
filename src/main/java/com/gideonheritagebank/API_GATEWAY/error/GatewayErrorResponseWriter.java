package com.gideonheritagebank.API_GATEWAY.error;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class GatewayErrorResponseWriter {

    public static final String REQUEST_ID_ATTRIBUTE =
            GatewayErrorResponseWriter.class.getName() + ".requestId";
    private final ObjectMapper objectMapper;

    public GatewayErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(
            ServerWebExchange exchange,
            HttpStatus status,
            String code,
            String message) {

        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.empty();
        }

        String requestId = exchange.getAttribute(REQUEST_ID_ATTRIBUTE);

        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        ApiErrorResponse error = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                exchange.getRequest().getPath().value(),
                requestId
        );

        byte[] bytes;

        try {
            bytes = objectMapper.writeValueAsString(error)
                    .getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException exception) {
            // Safe minimal JSON fallback if serialization fails.
            bytes = ("{\"status\":" + status.value()
                    + ",\"code\":\"INTERNAL_ERROR\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().setContentLength(bytes.length);

        return response.writeWith(Mono.just(
                response.bufferFactory().wrap(bytes)
        ));
    }
}