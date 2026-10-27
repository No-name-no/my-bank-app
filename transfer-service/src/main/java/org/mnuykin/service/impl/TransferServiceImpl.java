package org.mnuykin.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mnuykin.dto.NotificationsRq;
import org.mnuykin.dto.TranKind;
import org.mnuykin.dto.TransactionRq;
import org.mnuykin.dto.TransferRq;
import org.mnuykin.dto.TransferRs;
import org.mnuykin.producer.NotificationProducer;
import org.mnuykin.service.AccountsClient;
import org.mnuykin.service.TransferService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferServiceImpl implements TransferService {

    private static final String OPERATION_TRANSFER = "TRANSFER";

    private final AccountsClient accountsClient;
    private final NotificationProducer notificationProducer;
    private final MeterRegistry meterRegistry;

    @Override
    public Mono<TransferRs> transfer(String senderLogin, TransferRq rq) {
        validate(senderLogin, rq);

        String recipientLogin = rq.getRecipientLogin();
        BigDecimal amount = rq.getAmount();

        log.info("Transfer requested: {} -> {}, amount={}", senderLogin, recipientLogin, amount);

        TransactionRq transaction = new TransactionRq(
                TranKind.TRANSFER, amount, senderLogin, recipientLogin);

        return accountsClient.transaction(transaction)
                .then(accountsClient.getAccount(senderLogin))
                .flatMap(sender -> notificationProducer
                        .send(new NotificationsRq(
                                senderLogin,
                                "Перевод средств",
                                "Перевод " + amount + " руб. на счёт "
                                        + recipientLogin + " выполнен")
                        ).doOnError(throwable -> {
                                    log.warn(
                                            "Не удалось отправить уведомление об обновлении профиля, login={}",
                                            recipientLogin, throwable
                                    );
                                    Counter.builder("notification.failures")
                                            .description("Невозможность отправки уведомления")
                                            .tag("login", recipientLogin)
                                            .register(meterRegistry)
                                            .increment();
                                }
                        ).onErrorResume(throwable -> Mono.empty())
                        .thenReturn(new TransferRs(
                                sender.getLogin(),
                                recipientLogin,
                                amount,
                                sender.getBalance(),
                                OPERATION_TRANSFER)));
    }

    private void validate(String senderLogin, TransferRq rq) {
        if (senderLogin == null || senderLogin.isBlank()) {
            throw new IllegalArgumentException("Логин отправителя не определён");
        }
        if (rq.getRecipientLogin() == null || rq.getRecipientLogin().isBlank()) {
            throw new IllegalArgumentException("Логин получателя обязателен");
        }
        if (senderLogin.equals(rq.getRecipientLogin())) {
            throw new IllegalArgumentException("Нельзя переводить на свой же счёт");
        }
        if (rq.getAmount() == null) {
            throw new IllegalArgumentException("Сумма обязательна");
        }
        if (rq.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть больше нуля");
        }
    }
}