package org.mnuykin.service;

import lombok.extern.slf4j.Slf4j;
import org.mnuykin.dto.AccountDto;
import org.mnuykin.dto.ErrorRs;
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

    private static final String CODE_ACCOUNT_NOT_FOUND   = "AccountNotFound";
    private static final String CODE_INSUFFICIENT_FUNDS  = "InsufficientFunds";
    private static final String CODE_VALIDATION          = "Validation";
    private static final String CODE_ILLEGAL_ARGUMENT    = "IllegalArgument";
    private static final String CODE_UNKNOWN             = "Unknown";

    public AccountsClient(@Qualifier("accountsWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Void> transaction(TransactionRq rq) {
        return webClient.post()
                .uri("/internal/accounts/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(rq)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> mapError(clientResponse,
                        rq.getSrcLogin() + (rq.getDstLogin() != null ? " или " + rq.getDstLogin() : ""))
                ).bodyToMono(Void.class)
                .timeout(TIMEOUT)
                .doOnError(e -> log.warn("transaction {} failed: {}",
                        rq.getKind(), e.getMessage()));
    }

    public Mono<AccountDto> getAccount(String login) {
        return webClient.get()
                .uri("/internal/accounts/{login}", login)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> mapError(clientResponse, login))
                .bodyToMono(AccountDto.class)
                .timeout(TIMEOUT)
                .doOnError(e -> log.warn("getAccount({}) failed: {}", login, e.getMessage()));
    }

    private Mono<? extends Throwable> mapError(ClientResponse resp, String contextLogin) {
        return resp.bodyToMono(ErrorRs.class)
                .onErrorResume(ex -> {
                    log.warn("Failed to parse ErrorRs from accounts-service ({}): {}",
                            resp.statusCode().value(), ex.getMessage());
                    return Mono.empty();
                })
                .defaultIfEmpty(new ErrorRs(CODE_UNKNOWN, null))
                .map(rs -> mapError(rs, resp.statusCode(), contextLogin));
    }

    private Throwable mapError(ErrorRs rs, HttpStatusCode status, String loginForContext) {
        final String code = rs.getCode();
        final String msg  = rs.getMessage();

        if (code != null) {
            return switch (code) {
                case CODE_ACCOUNT_NOT_FOUND -> new AccountNotFoundException(orDefault(msg,
                        "Аккаунт не найден: " + loginForContext));
                case CODE_INSUFFICIENT_FUNDS -> new InsufficientFundsException(orDefault(msg,
                        "Недостаточно средств на счёте " + loginForContext));
                case CODE_VALIDATION, CODE_ILLEGAL_ARGUMENT ->
                        new IllegalArgumentException("accounts-service rejected request: " + orDefault(msg, code));
                default -> new DownstreamServiceException("accounts-service [" + code + "] " + status.value()
                        + (msg != null ? ": " + msg : ""));
            };
        }

        return new DownstreamServiceException(
                "accounts-service returned " + status.value());
    }

    private static String orDefault(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}