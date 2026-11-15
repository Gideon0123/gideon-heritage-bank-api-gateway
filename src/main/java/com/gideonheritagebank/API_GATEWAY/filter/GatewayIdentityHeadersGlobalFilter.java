package com.gideonheritagebank.API_GATEWAY.filter;

import com.gideonheritagebank.API_GATEWAY.utils.GatewayHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class GatewayIdentityHeadersGlobalFilter implements GlobalFilter, Ordered {

    private static final String LEGACY_USER_ID_HEADER = "X-Authenticated-User-Id";
    private static final String LEGACY_ROLES_HEADER = "X-Authenticated-Roles";
    private static final String CORRELATION_ID_PATTERN = "[A-Za-z0-9._:-]{1,128}";

    @Override
    public int getOrder() {
        // Execute late among Gateway global filters.
        // Spring Security authenticates the JWT in its WebFilter chain
        // before the request reaches the Gateway filter chain.
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        return exchange.getPrincipal()
                .ofType(JwtAuthenticationToken.class)
                .map(authentication ->
                        mutateHeaders(exchange, authentication.getToken())
                )
                .switchIfEmpty(
                        Mono.fromSupplier(() ->
                                mutateHeaders(exchange, null)
                        )
                )
                .flatMap(chain::filter);
    }

    private ServerWebExchange mutateHeaders(
            ServerWebExchange exchange,
            Jwt jwt
    ) {
        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers -> {

                    // Always remove caller-supplied identity metadata.
                    removeUntrustedIdentityHeaders(headers);

                    // Populate identity metadata only from a verified JWT.
                    if (jwt != null) {

                        String userId = jwt.getSubject();

                        if (userId != null && !userId.isBlank()) {
                            headers.set(
                                    GatewayHeaders.USER_ID,
                                    userId
                            );
                        }

                        String email =
                                jwt.getClaimAsString("email");

                        setHeader(
                                headers,
                                GatewayHeaders.USER_EMAIL,
                                email
                        );

                        String roles = extractRoles(jwt);

                        setHeader(
                                headers,
                                GatewayHeaders.USER_ROLE,
                                roles
                        );
                    }

                    // Preserve a valid incoming correlation ID.
                    // Otherwise, fall back to X-Request-Id or generate one.
                    String correlationId = resolveCorrelationId(exchange);

                    headers.set(
                            GatewayHeaders.CORRELATION_ID,
                            correlationId
                    );
                })
                .build();

        return exchange.mutate()
                .request(request)
                .build();
    }

    private void removeUntrustedIdentityHeaders(
            org.springframework.http.HttpHeaders headers
    ) {
        headers.remove(GatewayHeaders.USER_ID);
        headers.remove(GatewayHeaders.USER_EMAIL);
        headers.remove(GatewayHeaders.USER_ROLE);

        // Remove the old header convention too.
        headers.remove(LEGACY_USER_ID_HEADER);
        headers.remove(LEGACY_ROLES_HEADER);
    }

    private void setHeader(
            org.springframework.http.HttpHeaders headers,
            String name,
            String value
    ) {
        if (value != null && !value.isBlank()) {
            headers.set(name, value);
        }
    }

    private String extractRoles(Jwt jwt) {

        Object rolesClaim = jwt.getClaims().get("roles");

        if (rolesClaim instanceof Collection<?> roles) {
            return roles.stream()
                    .filter(Objects::nonNull)
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(role -> !role.isBlank())
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining(","));
        }

        if (rolesClaim instanceof String role && !role.isBlank()) {
            return role;
        }

        return null;
    }

    private String resolveCorrelationId(
            ServerWebExchange exchange
    ) {
        String incomingCorrelationId =
                exchange.getRequest()
                        .getHeaders()
                        .getFirst(GatewayHeaders.CORRELATION_ID);

        if (isValidCorrelationId(incomingCorrelationId)) {
            return incomingCorrelationId;
        }

        String requestId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-Request-Id");

        if (isValidCorrelationId(requestId)) {
            return requestId;
        }

        return UUID.randomUUID().toString();
    }

    private boolean isValidCorrelationId(String value) {
        return value != null
                && value.matches(CORRELATION_ID_PATTERN);
    }
}
