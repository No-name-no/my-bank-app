package org.mnuykin.contract;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@TestConfiguration
public class ContractJwtConfig {

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        return token -> {
            Instant now = Instant.now();
            return Mono.just(Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "test-user")
                    .claim("preferred_username", "login1")
                    .claim("realm_access", Map.of("roles", List.of("USER", "SERVICE")))
                    .claim("resource_access", Map.of(
                            "accounts-service", Map.of("roles", List.of("ACCOUNTS_WRITE"))))
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(300))
                    .build());
        };
    }
}