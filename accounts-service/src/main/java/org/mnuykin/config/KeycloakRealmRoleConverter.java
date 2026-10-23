package org.mnuykin.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KeycloakRealmRoleConverter implements Converter<Jwt, Flux<GrantedAuthority>> {

    @Override
    public Flux<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Нужен для определения ролей: realm_access.roles → ROLE_USER, ROLE_SERVICE
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
            roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
        }

        // Нужен для определения доступа к ресурсам: resource_access.<client_id>.roles → <client_id>_<ROLE>
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            resourceAccess.forEach((clientId, data) -> {
                if (data instanceof Map<?, ?> m && m.get("roles") instanceof List<?> roles) {
                    roles.forEach(r -> authorities.add(
                            new SimpleGrantedAuthority(clientId + "_" + r)));
                }
            });
        }

        return Flux.fromIterable(authorities);
    }
}