package com.gideonheritagebank.API_GATEWAY.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final JwtGrantedAuthoritiesConverter scopesConverter =
            new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {

        // Preserve standard scope/scp mappings such as SCOPE_accounts:read.
        Collection<GrantedAuthority> scopeAuthorities = scopesConverter.convert(jwt);

        Collection<GrantedAuthority> authorities = new ArrayList<>(scopeAuthorities);

        Object rolesClaim = jwt.getClaims().get("roles");

        if (rolesClaim instanceof Collection<?> roles) {
            for (Object role : roles) {
                if (role instanceof String roleName && !roleName.isBlank()) {
                    String normalizedRole = roleName.startsWith("ROLE_")
                            ? roleName : "ROLE_" + roleName;

                    authorities.add(new SimpleGrantedAuthority(normalizedRole));
                }
            }
        }

        // Do not trust roles sent in HTTP headers.
        // Only roles inside a successfully verified JWT are used.
        return List.copyOf(authorities);
    }
}
