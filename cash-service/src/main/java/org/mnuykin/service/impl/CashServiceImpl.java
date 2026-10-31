package org.mnuykin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mnuykin.dto.*;
import org.mnuykin.entity.OutboxEvent;
import org.mnuykin.service.AccountsClient;
import org.mnuykin.service.CashService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashServiceImpl implements CashService {

    private static final String OPERATION_DEPOSIT = "DEPOSIT";
    private static final String OPERATION_WITHDRAW = "WITHDRAW";

    private final AccountsClient accountsClient;

    private final ObjectMapper objectMapper;

    private final org.mnuykin.repository.OutboxRepository outboxRepository;

    @Override
    public Mono<CashRs> deposit(String login, BigDecimal amount) {
        validateAmount(amount);
        log.info("Deposit requested: login={}, amount={}", login, amount);

        TransactionRq rq = new TransactionRq(
                TranKind.CASH_DEPOSIT, amount, login, null, UUID.randomUUID().toString());

        return accountsClient.transaction(rq)
                .then(accountsClient.getAccount(login))
                .flatMap(account -> saveNotificationToOutbox(
                        new NotificationsRq(
                                login,
                                "Пополнение счёта",
                                "Счёт пополнен на " + amount + " руб."
                        ), login
                ).thenReturn(new CashRs(
                                account.getLogin(),
                                account.getBalance(),
                                OPERATION_DEPOSIT)));
    }

    @Override
    public Mono<CashRs> withdraw(String login, BigDecimal amount) {
        validateAmount(amount);
        log.info("Withdraw requested: login={}, amount={}", login, amount);

        TransactionRq rq = new TransactionRq(
                TranKind.CASH_WITHDRAWAL, amount, login, null, UUID.randomUUID().toString());

        return accountsClient.transaction(rq)
                .then(accountsClient.getAccount(login))
                .flatMap(account -> saveNotificationToOutbox(
                        new NotificationsRq(
                                login,
                                "Снятие со счёта",
                                "Со счёта снято " + amount + " руб."
                        ), login
                ).thenReturn(new CashRs(
                                account.getLogin(),
                                account.getBalance(),
                                OPERATION_WITHDRAW)));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Сумма обязательна");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть больше нуля");
        }
    }

    private Mono<Void> saveNotificationToOutbox(NotificationsRq rq, String login) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(rq))
                .map(Json::of)
                .flatMap(json -> {
                    OutboxEvent event = new OutboxEvent();
                    event.setLogin(login);
                    event.setPayload(json);
                    event.setStatus(OutboxStatus.PENDING);
                    event.setUpdateAt(OffsetDateTime.now());
                    return outboxRepository.save(event);
                }).then();
    }
}