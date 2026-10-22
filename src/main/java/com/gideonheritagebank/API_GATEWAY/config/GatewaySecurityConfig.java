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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain gatewaySecurityFilterChain(
            ServerHttpSecurity http,
            JwtAuthoritiesConverter authoritiesConverter,
            CookieOrHeaderBearerTokenConverter tokenConverter,
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            JsonAccessDeniedHandler accessDeniedHandler) {

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
                .csrf(csrf -> csrf
                        .csrfTokenRepository(
                                CookieServerCsrfTokenRepository.withHttpOnlyFalse()
                        )
                )

                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)

                .authorizeExchange(exchanges -> exchanges

                        .pathMatchers(
                                HttpMethod.GET,
                                "/actuator/health",
                                "/actuator/health/**"
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

                        .pathMatchers("/actuator/**").denyAll()

                        .anyExchange().authenticated()
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

        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256).build();

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
