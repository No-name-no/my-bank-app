package org.mnuykin.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mnuykin.service.NotificationClient;
import org.mnuykin.dto.CashRs;
import org.mnuykin.dto.NotificationsRq;
import org.mnuykin.dto.TranKind;
import org.mnuykin.dto.TransactionRq;
import org.mnuykin.service.AccountsClient;
import org.mnuykin.service.CashService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashServiceImpl implements CashService {

    private static final String OPERATION_DEPOSIT = "DEPOSIT";
    private static final String OPERATION_WITHDRAW = "WITHDRAW";

    private final AccountsClient accountsClient;
    private final NotificationClient notificationClient;

    @Override
    public Mono<CashRs> deposit(String login, BigDecimal amount) {
        validateAmount(amount);
        log.info("Deposit requested: login={}, amount={}", login, amount);

        TransactionRq rq = new TransactionRq(
                TranKind.CASH_DEPOSIT, amount, login, null);

        return accountsClient.transaction(rq)
                .then(accountsClient.getAccount(login))
                .flatMap(account -> notificationClient
                        .send(new NotificationsRq(
                                login,
                                "Пополнение счёта",
                                "Счёт пополнен на " + amount + " руб."))
                        .thenReturn(new CashRs(
                                account.getLogin(),
                                account.getBalance(),
                                OPERATION_DEPOSIT)));
    }

    @Override
    public Mono<CashRs> withdraw(String login, BigDecimal amount) {
        validateAmount(amount);
        log.info("Withdraw requested: login={}, amount={}", login, amount);

        TransactionRq rq = new TransactionRq(
                TranKind.CASH_WITHDRAWAL, amount, login, null);

        return accountsClient.transaction(rq)
                .then(accountsClient.getAccount(login))
                .flatMap(account -> notificationClient
                        .send(new NotificationsRq(
                                login,
                                "Снятие со счёта",
                                "Со счёта снято " + amount + " руб."))
                        .thenReturn(new CashRs(
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
}