package com.gideonheritagebank.API_GATEWAY.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component("globalRateLimitKeyResolver")
public class GlobalRateLimitKeyResolver implements KeyResolver {

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {

        return exchange.getPrincipal()
                .filter(principal ->
                        principal.getName() != null && !principal.getName().isBlank()
                )
//                .filter(principal -> !principal.isBlank())
                .filter(Authentication.class::isInstance)
                .map(Authentication.class::cast)
                .filter(Authentication::isAuthenticated)
                .map(authentication ->
                        "user:" + authentication.getName()
                )
                .switchIfEmpty(Mono.fromSupplier(() -> {
                    if (exchange.getRequest().getRemoteAddress() == null
                            || exchange.getRequest().getRemoteAddress()
                            .getAddress() == null) {
                        return "ip:unknown";
                    }

                    String clientIp = exchange.getRequest()
                            .getRemoteAddress()
                            .getAddress()
                            .getHostAddress();

                    return "ip:" + clientIp;
                }));
    }
}