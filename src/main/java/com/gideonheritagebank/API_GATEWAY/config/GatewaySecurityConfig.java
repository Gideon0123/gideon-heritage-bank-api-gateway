package com.gideonheritagebank.API_GATEWAY.config;

import java.util.List;

import com.gideonheritagebank.API_GATEWAY.error.JsonAccessDeniedHandler;
import com.gideonheritagebank.API_GATEWAY.error.JsonAuthenticationEntryPoint;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:4200"
        ));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Request-Id",
                "X-XSRF-TOKEN"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public SecurityWebFilterChain gatewaySecurityFilterChain(
            ServerHttpSecurity http,
            JwtAuthoritiesConverter authoritiesConverter,
            CookieOrHeaderBearerTokenConverter tokenConverter,
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            JsonAccessDeniedHandler accessDeniedHandler,
            CorsConfigurationSource corsConfigurationSource) {

        JwtAuthenticationConverter jwtAuthenticationConverter =
                new JwtAuthenticationConverter();

        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        ReactiveJwtAuthenticationConverterAdapter reactiveConverter =
                new ReactiveJwtAuthenticationConverterAdapter(
                        jwtAuthenticationConverter
                );

        return http
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource)
                )

                .csrf(csrf -> csrf
                        .csrfTokenRepository(
                                CookieServerCsrfTokenRepository.withHttpOnlyFalse()
                        )

                        // Accept the plain CSRF token supplied in the
                        // X-XSRF-TOKEN header by Postman or a frontend.
                        .csrfTokenRequestHandler(
                                new ServerCsrfTokenRequestAttributeHandler()
                        )

                        // Ensure CSRF failures use our JSON error handler.
                        .accessDeniedHandler(accessDeniedHandler)

                        .requireCsrfProtectionMatcher(exchange -> {
                            String path = exchange.getRequest()
                                    .getURI()
                                    .getPath();

                            HttpMethod method = exchange.getRequest()
                                    .getMethod();

                            boolean isSafeMethod =
                                    method == HttpMethod.GET
                                            || method == HttpMethod.HEAD
                                            || method == HttpMethod.OPTIONS
                                            || method == HttpMethod.TRACE;

                            boolean isPublicAuthEndpoint =
                                    path.equals("/api/v1/identity/auth/register")
                                            || path.equals("/api/v1/identity/auth/login")
                                            || path.equals("/api/v1/identity/auth/logout")
                                            || path.equals("/api/v1/identity/auth/verify-email")
                                            || path.equals("/api/v1/identity/auth/forgot-password")
                                            || path.equals("/api/v1/identity/auth/reset-password")
                                            || path.equals("/api/v1/identity/auth/refresh-token");

                            boolean requireCsrf =
                                    !isSafeMethod && !isPublicAuthEndpoint;

                            return requireCsrf
                                    ? ServerWebExchangeMatcher.MatchResult.match()
                                    : ServerWebExchangeMatcher.MatchResult.notMatch();
                        })
                )

                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)

                .authorizeExchange(exchanges -> exchanges

                        // CORS preflight requests.
                        .pathMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()

                        .pathMatchers(
                                HttpMethod.GET,
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/gateway/routes",
                                "/actuator/gateway/routes/**"
                        ).permitAll()

                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/v1/identity/auth/login",
                                "/api/v1/identity/auth/register",
                                "/api/v1/identity/auth/verify-email",
                                "/api/v1/identity/auth/forgot-password",
                                "/api/v1/identity/auth/reset-password",
                                "/api/v1/identity/auth/refresh-token"
                        ).permitAll()

                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/v1/identity/auth/csrf"
                        ).permitAll()

                        .pathMatchers("/api/v1/identity/staff/**")
                        .hasAnyAuthority(
                                "ROLE_STAFF",
                                "ROLE_BRANCH_MANAGER",
                                "ROLE_ADMIN"
                        )

                        .pathMatchers("/api/v1/identity/admin/**")
                        .hasAuthority("ROLE_ADMIN")

                        .pathMatchers("/api/v1/risk/**")
                        .hasAnyAuthority(
                                "ROLE_ADMIN",
                                "ROLE_RISK_ANALYST",
                                "SCOPE_risk:read"
                        )

                        .pathMatchers(HttpMethod.GET, "/csrf")
                        .permitAll()

                        .pathMatchers("/fallback/**")
                        .permitAll()

                        .pathMatchers("/actuator/**")
                        .denyAll()

                        .pathMatchers("/api/v1/**")
                        .authenticated()

                        .anyExchange()
                        .permitAll()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenConverter(tokenConverter)
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(reactiveConverter)
                        )
                )

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                .build();
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(
            @Value("${bank.security.jwt.jwk-set-uri}")
            String jwkSetUri,

            @Value("${bank.security.jwt.issuer-uri}")
            String issuerUri,

            @Value("${bank.security.jwt.audience}")
            String audience) {

        NimbusReactiveJwtDecoder decoder =
                NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri)
                        .jwsAlgorithm(SignatureAlgorithm.RS256)
                        .build();

        OAuth2TokenValidator<Jwt> issuerAndTimestampValidator =
                JwtValidators.createDefaultWithIssuer(issuerUri);

        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            List<String> audiences = jwt.getAudience();

            if (audiences != null && audiences.contains(audience)) {
                return OAuth2TokenValidatorResult.success();
            }

            OAuth2Error error = new OAuth2Error(
                    "invalid_token",
                    "The token audience is not trusted by this API.",
                    null
            );

            return OAuth2TokenValidatorResult.failure(error);
        };

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        issuerAndTimestampValidator,
                        audienceValidator
                )
        );

        return decoder;
    }
}