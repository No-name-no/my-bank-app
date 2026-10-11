package org.mnuykin.client;

import lombok.extern.slf4j.Slf4j;
import org.mnuykin.dto.NotificationsRq;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@Slf4j
public class NotificationClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public NotificationClient(@Qualifier("notificationsWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Void> send(NotificationsRq request) {
        return webClient.post()
                .uri("/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(err -> Mono.error(new IllegalStateException(
                                        "notifications-service " + resp.statusCode() + ": " + err))))
                .bodyToMono(Void.class)
                .timeout(TIMEOUT)
                .doOnError(e -> log.warn(
                        "Уведомление не отправлено (login={}, subject={}): {}",
                        request.login(), request.subject(), e.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }
}