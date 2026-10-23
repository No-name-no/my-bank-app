package org.mnuykin.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class BearerTokenRelayGatewayFilterFactory
        extends AbstractGatewayFilterFactory<BearerTokenRelayGatewayFilterFactory.Config> {

    public BearerTokenRelayGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .map(Jwt::getTokenValue)
                .map(token -> exchange.mutate()
                        .request(r -> r.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                        .build())
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    public static class Config {
        // параметров нет
    }
}