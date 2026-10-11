package org.mnuykin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mnuykin.dto.AccountDto;
import org.mnuykin.dto.TranKind;
import org.mnuykin.dto.TransactionRq;
import org.mnuykin.dto.TransferRq;
import org.mnuykin.exception.InsufficientFundsException;
import org.mnuykin.service.AccountsClient;
import org.mnuykin.service.NotificationClient;
import org.mnuykin.service.impl.TransferServiceImpl;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock AccountsClient accountsClient;
    @Mock NotificationClient notificationClient;
    @InjectMocks TransferServiceImpl service;

    @Test
    void transfer_happyPath() {
        when(accountsClient.transaction(any())).thenReturn(Mono.empty());
        when(accountsClient.getAccount("login1")).thenReturn(Mono.just(
                new AccountDto("login1", "Федя", "Сумкин",
                        LocalDate.of(1990, 1, 1), new BigDecimal("900.00"))));
        when(notificationClient.send(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.transfer("login1",
                        new TransferRq("login2", new BigDecimal("100"))))
                .assertNext(r -> {
                    assertThat(r.getSenderLogin()).isEqualTo("login1");
                    assertThat(r.getRecipientLogin()).isEqualTo("login2");
                    assertThat(r.getAmount()).isEqualByComparingTo("100");
                    assertThat(r.getSenderBalance()).isEqualByComparingTo("900.00");
                    assertThat(r.getOperation()).isEqualTo("TRANSFER");
                })
                .verifyComplete();

        ArgumentCaptor<TransactionRq> captor = ArgumentCaptor.forClass(TransactionRq.class);
        verify(accountsClient).transaction(captor.capture());
        TransactionRq sent = captor.getValue();
        assertThat(sent.getKind()).isEqualTo(TranKind.TRANSFER);
        assertThat(sent.getSrcLogin()).isEqualTo("login1");
        assertThat(sent.getDstLogin()).isEqualTo("login2");
        assertThat(sent.getAmount()).isEqualByComparingTo("100");

        verify(notificationClient).send(any());
    }

    @Test
    void transfer_shouldNotSendNotificationWhenTransactionFails() {
        when(accountsClient.transaction(any()))
                .thenReturn(Mono.error(new InsufficientFundsException("no money")));

        StepVerifier.create(service.transfer("login1",
                        new TransferRq("login2", new BigDecimal("99999"))))
                .expectError(InsufficientFundsException.class)
                .verify();

        verifyNoInteractions(notificationClient);
        verify(accountsClient, never()).getAccount(any());
    }

    @Test
    void transfer_shouldRejectSelfTransfer() {
        StepVerifier.create(service.transfer("login1",
                        new TransferRq("login1", new BigDecimal("100"))))
                .expectError(IllegalArgumentException.class)
                .verify();

        verifyNoInteractions(accountsClient, notificationClient);
    }

    @Test
    void transfer_shouldRejectNonPositiveAmount() {
        StepVerifier.create(service.transfer("login1",
                        new TransferRq("login2", BigDecimal.ZERO)))
                .expectError(IllegalArgumentException.class)
                .verify();

        verifyNoInteractions(accountsClient, notificationClient);
    }
}