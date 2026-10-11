package org.mnuykin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${bank.accounts-service.base-url:http://accounts-service}")
    private String accountsServiceBaseUrl;

    @Value("${bank.notifications-service.base-url:http://notifications-service}")
    private String notificationsServiceBaseUrl;

    @Value("${bank.oauth2.registration.accounts:accounts}")
    private String accountsRegistrationId;

    @Value("${bank.oauth2.registration.notifications:notifications}")
    private String notificationsRegistrationId;

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
            ReactiveClientRegistrationRepository registrations,
            ReactiveOAuth2AuthorizedClientService service) {

        ReactiveOAuth2AuthorizedClientProvider provider =
                ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build();

        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(registrations, service);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    @Bean
    public WebClient accountsWebClient(
            @LoadBalanced WebClient.Builder builder,
            ReactiveOAuth2AuthorizedClientManager manager) {

        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(manager);
        oauth2.setDefaultClientRegistrationId(accountsRegistrationId);

        return builder.clone()
                .baseUrl(accountsServiceBaseUrl)
                .filter(oauth2)
                .build();
    }

    @Bean
    public WebClient notificationsWebClient(
            @LoadBalanced WebClient.Builder builder,
            ReactiveOAuth2AuthorizedClientManager manager) {

        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(manager);
        oauth2.setDefaultClientRegistrationId(notificationsRegistrationId);

        return builder.clone()
                .baseUrl(notificationsServiceBaseUrl)
                .filter(oauth2)
                .build();
    }
}