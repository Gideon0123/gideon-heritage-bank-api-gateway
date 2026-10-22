package com.gideonheritagebank.API_GATEWAY.config;

import org.springframework.http.HttpCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class CookieOrHeaderBearerTokenConverter implements ServerAuthenticationConverter {

    private static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";
    private final ServerBearerTokenAuthenticationConverter headerTokenConverter =
            new ServerBearerTokenAuthenticationConverter();

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {

        // Standard Authorization: Bearer authentication has priority.
        return headerTokenConverter.convert(exchange)
                .switchIfEmpty(Mono.defer(() -> {

                    HttpCookie accessTokenCookie =
                            exchange.getRequest()
                                    .getCookies()
                                    .getFirst(ACCESS_TOKEN_COOKIE);

                    if (accessTokenCookie == null || accessTokenCookie.getValue().isBlank()) {
                        return Mono.empty();
                    }

                    return Mono.just(
                            new BearerTokenAuthenticationToken(
                                    accessTokenCookie.getValue()
                            )
                    );
                }));
    }
}

