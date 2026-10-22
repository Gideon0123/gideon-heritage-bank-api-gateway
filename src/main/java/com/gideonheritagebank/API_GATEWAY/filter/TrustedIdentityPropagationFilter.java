package com.gideonheritagebank.API_GATEWAY.filter;

import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class TrustedIdentityPropagationFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER = "X-Authenticated-User-Id";
    private static final String ROLES_HEADER = "X-Authenticated-Roles";

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        // Never preserve identity headers provided by the client.
        ServerHttpRequest sanitizedRequest = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.remove("X-USER-ID");
                    headers.remove("X-USER-ROLE");
                    headers.remove(USER_ID_HEADER);
                    headers.remove(ROLES_HEADER);
                })
                .build();

        ServerWebExchange sanitizedExchange = exchange.mutate()
                .request(sanitizedRequest)
                .build();

        // Obtain authentication from Spring Security's reactive context.
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .filter(JwtAuthenticationToken.class::isInstance)
                .map(JwtAuthenticationToken.class::cast)
                .map(authentication -> {

                    var jwt = authentication.getToken();

                    String claimedUserId = jwt.getClaimAsString("user_id");

                    final String userId =
                            claimedUserId == null || claimedUserId.isBlank()
                                    ? jwt.getSubject() : claimedUserId;

                    String roles = authentication.getAuthorities()
                            .stream()
                            .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                            .filter(authority ->
                                    authority.startsWith("ROLE_"))
                            .sorted()
                            .collect(Collectors.joining(","));

                    ServerHttpRequest trustedRequest =
                            sanitizedExchange.getRequest()
                                    .mutate()
                                    .headers(headers -> {
                                        if (userId != null
                                                && !userId.isBlank()) {
                                            headers.set(
                                                    USER_ID_HEADER,
                                                    userId
                                            );
                                        }

                                        if (!roles.isBlank()) {
                                            headers.set(
                                                    ROLES_HEADER,
                                                    roles
                                            );
                                        }
                                    })
                                    .build();

                    return sanitizedExchange.mutate()
                            .request(trustedRequest)
                            .build();
                })
                .defaultIfEmpty(sanitizedExchange)
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
