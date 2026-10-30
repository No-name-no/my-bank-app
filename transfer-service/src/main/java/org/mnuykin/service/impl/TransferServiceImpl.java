package org.mnuykin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mnuykin.dto.*;
import org.mnuykin.entity.OutboxEvent;
import org.mnuykin.producer.NotificationProducer;
import org.mnuykin.service.AccountsClient;
import org.mnuykin.service.TransferService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferServiceImpl implements TransferService {

    private static final String OPERATION_TRANSFER = "TRANSFER";

    private final AccountsClient accountsClient;
    private final NotificationProducer notificationProducer;

    private final ObjectMapper objectMapper;

    private final org.mnuykin.repository.OutboxRepository outboxRepository;

    @Override
    public Mono<TransferRs> transfer(String senderLogin, TransferRq rq) {
        validate(senderLogin, rq);

        String recipientLogin = rq.getRecipientLogin();
        BigDecimal amount = rq.getAmount();

        log.info("Transfer requested: {} -> {}, amount={}", senderLogin, recipientLogin, amount);

        TransactionRq transaction = new TransactionRq(
                TranKind.TRANSFER, amount, senderLogin, recipientLogin, UUID.randomUUID().toString());

        return accountsClient.transaction(transaction)
                .then(accountsClient.getAccount(senderLogin))
                .flatMap(sender -> saveNotificationToOutbox(
                        new NotificationsRq(
                                senderLogin,
                                "Перевод средств",
                                "Перевод " + amount + " руб. на счёт "
                                        + recipientLogin + " выполнен"
                        ), senderLogin
                ).thenReturn(new TransferRs(
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