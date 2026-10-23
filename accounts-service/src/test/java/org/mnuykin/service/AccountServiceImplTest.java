package org.mnuykin.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mnuykin.dto.AccountUpdateRq;
import org.mnuykin.dto.TranKind;
import org.mnuykin.dto.TransactionRq;
import org.mnuykin.entity.Account;
import org.mnuykin.exception.AccountNotFoundException;
import org.mnuykin.exception.InsufficientFundsException;
import org.mnuykin.mapper.AccountMapper;
import org.mnuykin.repository.AccountRepository;
import org.mnuykin.service.impl.AccountServiceImpl;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock AccountRepository repository;
    @Mock NotificationClient notificationClient;
    @Mock AccountMapper mapper;
    @InjectMocks AccountServiceImpl service;

    @Test
    void getByLogin_shouldThrow_whenNotFound() {
        when(repository.findByLogin("ghost")).thenReturn(Mono.empty());

        StepVerifier.create(service.getByLogin("ghost"))
                .expectError(AccountNotFoundException.class)
                .verify();
    }

    @Test
    void updateByLogin_shouldRejectUnderage() {
        AccountUpdateRq rq = new AccountUpdateRq("Фёдор", "Сумкин",
                LocalDate.now().minusYears(15));
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateByLogin("login1", rq)
        );
        assertEquals("Возраст должен быть не менее 18 лет", exception.getMessage());

        verifyNoInteractions(repository, notificationClient);
    }

    @Test
    void deposit_shouldIncrement() {
        when(repository.incrementBalance("login1", new BigDecimal("100")))
                .thenReturn(Mono.just(1));

        StepVerifier.create(service.execTran(new TransactionRq(
                        TranKind.CASH_DEPOSIT, new BigDecimal("100"), "login1", null)))
                .verifyComplete();
    }

    @Test
    void deposit_shouldThrow_whenAccountNotFound() {
        when(repository.incrementBalance("ghost", new BigDecimal("100")))
                .thenReturn(Mono.just(0));

        StepVerifier.create(service.execTran(new TransactionRq(
                        TranKind.CASH_DEPOSIT, new BigDecimal("100"), "ghost", null)))
                .expectError(AccountNotFoundException.class)
                .verify();
    }

    @Test
    void withdraw_shouldDecrement_whenEnoughMoney() {
        when(repository.decrementBalance("login1", new BigDecimal("100")))
                .thenReturn(Mono.just(1));

        StepVerifier.create(service.execTran(new TransactionRq(
                        TranKind.CASH_WITHDRAWAL, new BigDecimal("100"), "login1", null)))
                .verifyComplete();
    }

    @Test
    void withdraw_shouldThrow_whenInsufficientFunds() {
        when(repository.decrementBalance("login1", new BigDecimal("99999")))
                .thenReturn(Mono.just(0));
        when(repository.findByLogin("login1"))
                .thenReturn(Mono.just(Account.builder().login("login1").build()));

        StepVerifier.create(service.execTran(new TransactionRq(
                        TranKind.CASH_WITHDRAWAL, new BigDecimal("99999"), "login1", null)))
                .expectError(InsufficientFundsException.class)
                .verify();
    }

    @Test
    void withdraw_shouldThrow_whenAccountMissing() {
        when(repository.decrementBalance("ghost", new BigDecimal("100")))
                .thenReturn(Mono.just(0));
        when(repository.findByLogin("ghost")).thenReturn(Mono.empty());

        StepVerifier.create(service.execTran(new TransactionRq(
                        TranKind.CASH_WITHDRAWAL, new BigDecimal("100"), "ghost", null)))
                .expectError(AccountNotFoundException.class)
                .verify();
    }

    @Test
    void transfer_shouldDecrementThenIncrement() {
        when(repository.decrementBalance("login1", new BigDecimal("100")))
                .thenReturn(Mono.just(1));
        when(repository.incrementBalance("login2", new BigDecimal("100")))
                .thenReturn(Mono.just(1));

        StepVerifier.create(service.execTran(new TransactionRq(
                        TranKind.TRANSFER, new BigDecimal("100"), "login1", "login2")))
                .verifyComplete();

        // Проверяем порядок: сначала decrement, потом increment
        var inOrder = inOrder(repository);
        inOrder.verify(repository).decrementBalance("login1", new BigDecimal("100"));
        inOrder.verify(repository).incrementBalance("login2", new BigDecimal("100"));
    }

    @Test
    void transfer_shouldNotIncrement_whenDecrementFails() {
        when(repository.decrementBalance("login1", new BigDecimal("99999")))
                .thenReturn(Mono.just(0));
        when(repository.findByLogin("login1"))
                .thenReturn(Mono.just(Account.builder().login("login1").build()));

        StepVerifier.create(service.execTran(new TransactionRq(
                        TranKind.TRANSFER, new BigDecimal("99999"), "login1", "login2")))
                .expectError(InsufficientFundsException.class)
                .verify();

        verify(repository, never()).incrementBalance(any(), any());
    }

    @Test
    void transfer_shouldRejectSelfTransfer() {
        StepVerifier.create(service.execTran(new TransactionRq(
                        TranKind.TRANSFER, new BigDecimal("100"), "login1", "login1")))
                .expectError(IllegalArgumentException.class)
                .verify();

        verifyNoInteractions(repository);
    }
}