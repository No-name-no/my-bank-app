package org.mnuykin.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mnuykin.dto.*;
import org.mnuykin.entity.Account;
import org.mnuykin.exception.AccountNotFoundException;
import org.mnuykin.exception.InsufficientFundsException;
import org.mnuykin.mapper.AccountMapper;
import org.mnuykin.repository.AccountRepository;
import org.mnuykin.service.AccountService;
import org.mnuykin.service.NotificationClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {
    private static final int MIN_AGE = 18;

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final NotificationClient notificationClient;

    @Override
    public Mono<AccountDto> getByLogin(String login) {
        return findByLoginOrThrow(login).map(accountMapper::toDto);
    }

    @Override
    @Transactional
    public Mono<AccountDto> updateByLogin(String login, AccountUpdateRq updateRq) {
        validateAge(updateRq.getBirthday());
        return findByLoginOrThrow(login).flatMap(account -> {
                    account.setFirstName(updateRq.getFirstName());
                    account.setLastName(updateRq.getLastName());
                    account.setBirthDate(updateRq.getBirthday());
                    return accountRepository.save(account);
                }).flatMap(saved -> notificationClient.send(new NotificationsRq(saved.getLogin(),
                                "Профиль обновлён",
                                "Данные аккаунта изменены: " + saved.getFirstName() + " " + saved.getLastName())
                        ).thenReturn(saved)
                ).map(accountMapper::toDto);
    }

    @Override
    public Flux<AccountShortDto> getOtherAccounts(String login) {
        return accountRepository.findAllByLoginNot(login).map(accountMapper::toShortDto);
    }

    @Override
    @Transactional
    public Mono<Void> execTran(TransactionRq transactionRq) {
        final TranKind kind = transactionRq.getKind();
        return switch (kind) {
            case CASH_WITHDRAWAL -> executeDecrement(transactionRq.getSrcLogin(), transactionRq.getAmount());
            case CASH_DEPOSIT    -> executeIncrement(transactionRq.getSrcLogin(), transactionRq.getAmount());
            case TRANSFER        -> executeTransfer(transactionRq.getSrcLogin(), transactionRq.getDstLogin(),
                        transactionRq.getAmount());
        };
    }

    private Mono<Void> executeTransfer(String srcLogin, String dstLogin, BigDecimal amount){
        if (srcLogin.equals(dstLogin)) {
            return Mono.error(new IllegalArgumentException("Нельзя переводить на тот же счёт"));
        }

        return Mono.zip(
                executeDecrement(srcLogin, amount),
                executeIncrement(dstLogin, amount)
        ).then();
    }

    private Mono<Void> executeIncrement(String login, BigDecimal amount){
        return accountRepository.incrementBalance(login, amount)
                .flatMap(result -> {
                    if (result == 0) return Mono.error(new AccountNotFoundException("Аккаунт не найден: " + login));
                    return Mono.empty();
                });
    }

    private Mono<Void> executeDecrement(String login, BigDecimal amount) {
        return accountRepository.decrementBalance(login, amount)
                .flatMap(updated -> {
                    if (updated > 0) {
                        return Mono.empty();
                    }
                    return accountRepository.findByLogin(login)
                            .switchIfEmpty(Mono.error(new AccountNotFoundException("Аккаунт не найден: " + login)))
                            .flatMap(a -> Mono.error(
                                    new InsufficientFundsException("Недостаточно средств на счёте " + login))
                            );
                });
    }

    private Mono<Account> findByLoginOrThrow(String login) {
        return accountRepository.findByLogin(login)
                .switchIfEmpty(Mono.error(new AccountNotFoundException("Аккаунт не найден: " + login)));
    }

    private void validateAge(LocalDate birthDate) {
        if (birthDate.isAfter(LocalDate.now().minusYears(MIN_AGE))) {
            throw new IllegalArgumentException("Возраст должен быть не менее " + MIN_AGE + " лет");
        }
    }
}
