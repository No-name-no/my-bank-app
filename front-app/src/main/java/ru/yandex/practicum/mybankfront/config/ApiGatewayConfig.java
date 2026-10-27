package ru.yandex.practicum.mybankfront.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class ApiGatewayConfig {

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrations,
            OAuth2AuthorizedClientRepository authorizedClients) {

        var provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .build();

        var manager = new DefaultOAuth2AuthorizedClientManager(
                clientRegistrations, authorizedClients);
        manager.setAuthorizedClientProvider(provider);

        return manager;
    }

    @Bean
    @Primary
    public RestClient.Builder regularRestClientBuilder(ObservationRegistry observationRegistry) {
        return RestClient.builder().observationRegistry(observationRegistry);
    }

    @Bean
    @LoadBalanced
    @Scope("prototype")
    public RestClient.Builder loadBalancedRestClientBuilder(ObservationRegistry observationRegistry) {
        return RestClient.builder().observationRegistry(observationRegistry);
    }

    @Bean
    public RestClient gatewayRestClient(
            @LoadBalanced RestClient.Builder builder,
            OAuth2AuthorizedClientManager manager,
            @Value("${services.gateway.url}") String baseUrl) {

        var oauth2Interceptor = new OAuth2ClientHttpRequestInterceptor(manager);
        oauth2Interceptor.setClientRegistrationIdResolver(req -> "keycloak");

        return builder.clone()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor(oauth2Interceptor)
                .build();
    }
}
