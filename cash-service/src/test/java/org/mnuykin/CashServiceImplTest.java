package org.mnuykin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mnuykin.producer.NotificationProducer;
import org.mnuykin.repository.OutboxRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mnuykin.dto.AccountDto;
import org.mnuykin.dto.TranKind;
import org.mnuykin.dto.TransactionRq;
import org.mnuykin.service.AccountsClient;
import org.mnuykin.service.impl.CashServiceImpl;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashServiceImplTest {

    @Mock AccountsClient accountsClient;
    @Mock
    NotificationProducer notificationClient;
    @InjectMocks CashServiceImpl service;
    @Mock
    OutboxRepository outboxRepository;
    @Mock
    ObjectMapper objectMapper;

    @Test
    void deposit_happyPath() {
        when(accountsClient.transaction(any())).thenReturn(Mono.empty());
        when(accountsClient.getAccount("login1")).thenReturn(Mono.just(
                new AccountDto("login1", "Федя", "Сумкин",
                        LocalDate.of(1990, 1, 1), new BigDecimal("1100.00"))));

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
    }

    @Test
    void withdraw_happyPath() {
        when(accountsClient.transaction(any())).thenReturn(Mono.empty());
        when(accountsClient.getAccount("login1")).thenReturn(Mono.just(
                new AccountDto("login1", "Федя", "Сумкин",
                        LocalDate.of(1990, 1, 1), new BigDecimal("900.00"))));

        StepVerifier.create(service.withdraw("login1", new BigDecimal("100.00")))
                .assertNext(r -> {
                    assertThat(r.getOperation()).isEqualTo("WITHDRAW");
                    assertThat(r.getBalance()).isEqualByComparingTo("900.00");
                })
                .verifyComplete();
    }

    @Test
    void deposit_shouldRejectNonPositiveAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.deposit("login1", BigDecimal.ZERO)
        );

        verifyNoInteractions(accountsClient, notificationClient);
    }
}