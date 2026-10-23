package org.mnuykin.service;

import lombok.extern.slf4j.Slf4j;
import org.mnuykin.dto.AccountDto;
import org.mnuykin.dto.TransactionRq;
import org.mnuykin.exception.AccountNotFoundException;
import org.mnuykin.exception.DownstreamServiceException;
import org.mnuykin.exception.InsufficientFundsException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@Slf4j
public class AccountsClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public AccountsClient(@Qualifier("accountsWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Void> transaction(TransactionRq rq) {
        return webClient.post()
                .uri("/internal/accounts/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(rq)
                .retrieve()
                .onStatus(s -> s.value() == 404, resp ->
                        Mono.error(new AccountNotFoundException(
                                "Аккаунт не найден: " + rq.getSrcLogin()
                                        + (rq.getDstLogin() != null ? " или " + rq.getDstLogin() : ""))))
                .onStatus(s -> s.value() == 400, resp ->
                        Mono.error(new InsufficientFundsException(
                                "Недостаточно средств на счёте " + rq.getSrcLogin())))
                .onStatus(HttpStatusCode::isError, this::mapError)
                .bodyToMono(Void.class)
                .timeout(TIMEOUT)
                .doOnError(e -> log.warn("transaction {} failed: {}",
                        rq.getKind(), e.getMessage()));
    }

    public Mono<AccountDto> getAccount(String login) {
        return webClient.get()
                .uri("/internal/accounts/{login}", login)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(s -> s.value() == 404, resp ->
                        Mono.error(new AccountNotFoundException("Аккаунт не найден: " + login)))
                .onStatus(HttpStatusCode::isError, this::mapError)
                .bodyToMono(AccountDto.class)
                .timeout(TIMEOUT)
                .doOnError(e -> log.warn("getAccount({}) failed: {}", login, e.getMessage()));
    }

    private Mono<? extends Throwable> mapError(ClientResponse resp) {
        return resp.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new DownstreamServiceException(
                        "accounts-service " + resp.statusCode() + ": " + body));
    }
}