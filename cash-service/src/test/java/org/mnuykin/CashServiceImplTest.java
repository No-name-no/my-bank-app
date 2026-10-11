package org.mnuykin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mnuykin.service.NotificationClient;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mnuykin.dto.AccountDto;
import org.mnuykin.dto.TranKind;
import org.mnuykin.dto.TransactionRq;
import org.mnuykin.exception.InsufficientFundsException;
import org.mnuykin.service.AccountsClient;
import org.mnuykin.service.impl.CashServiceImpl;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashServiceImplTest {

    @Mock AccountsClient accountsClient;
    @Mock
    NotificationClient notificationClient;
    @InjectMocks CashServiceImpl service;

    @Test
    void deposit_happyPath() {
        when(accountsClient.transaction(any())).thenReturn(Mono.empty());
        when(accountsClient.getAccount("login1")).thenReturn(Mono.just(
                new AccountDto("login1", "Федя", "Сумкин",
                        LocalDate.of(1990, 1, 1), new BigDecimal("1100.00"))));
        when(notificationClient.send(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.deposit("login1", new BigDecimal("100.00")))
                .assertNext(r -> {
                    assertThat(r.getOperation()).isEqualTo("DEPOSIT");
                    assertThat(r.getBalance()).isEqualByComparingTo("1100.00");
                    assertThat(r.getLogin()).isEqualTo("login1");
                })
                .verifyComplete();

        ArgumentCaptor<TransactionRq> captor = ArgumentCaptor.forClass(TransactionRq.class);
        verify(accountsClient).transaction(captor.capture());
        TransactionRq sent = captor.getValue();
        assertThat(sent.getKind()).isEqualTo(TranKind.CASH_DEPOSIT);
        assertThat(sent.getSrcLogin()).isEqualTo("login1");
        assertThat(sent.getDstLogin()).isNull();
        assertThat(sent.getAmount()).isEqualByComparingTo("100.00");

        verify(notificationClient).send(any());
    }

    @Test
    void withdraw_happyPath() {
        when(accountsClient.transaction(any())).thenReturn(Mono.empty());
        when(accountsClient.getAccount("login1")).thenReturn(Mono.just(
                new AccountDto("login1", "Федя", "Сумкин",
                        LocalDate.of(1990, 1, 1), new BigDecimal("900.00"))));
        when(notificationClient.send(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.withdraw("login1", new BigDecimal("100.00")))
                .assertNext(r -> {
                    assertThat(r.getOperation()).isEqualTo("WITHDRAW");
                    assertThat(r.getBalance()).isEqualByComparingTo("900.00");
                })
                .verifyComplete();
    }

    @Test
    void withdraw_shouldPropagateInsufficientFunds() {
        when(accountsClient.transaction(any()))
                .thenReturn(Mono.error(new InsufficientFundsException("no money")));

        StepVerifier.create(service.withdraw("login1", new BigDecimal("99999")))
                .expectError(InsufficientFundsException.class)
                .verify();

        verify(accountsClient, never()).getAccount(any());
        verifyNoInteractions(notificationClient);
    }

    @Test
    void deposit_shouldRejectNonPositiveAmount() {
        StepVerifier.create(service.deposit("login1", BigDecimal.ZERO))
                .expectError(IllegalArgumentException.class)
                .verify();

        verifyNoInteractions(accountsClient, notificationClient);
    }
}