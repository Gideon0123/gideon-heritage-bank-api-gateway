package com.gideonheritagebank.API_GATEWAY.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class CsrfController {

    @GetMapping("/csrf")
    public Mono<Map<String, String>> getCsrfToken(
            ServerWebExchange exchange) {

        Mono<CsrfToken> csrfToken =
                exchange.getAttribute(CsrfToken.class.getName());

        if (csrfToken == null) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "CSRF token could not be initialized"
            ));
        }

        return csrfToken.map(token -> Map.of(
                "headerName", token.getHeaderName(),
                "parameterName", token.getParameterName(),
                "token", token.getToken()
        ));
    }
}

