package org.mnuykin.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mnuykin.dto.*;
import org.mnuykin.exception.AccountNotFoundException;
import org.mnuykin.exception.InsufficientFundsException;
import org.mnuykin.mapper.AccountMapper;
import org.mnuykin.repository.AccountRepository;
import org.mnuykin.service.AccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {
    private AccountRepository accountRepository;
    private AccountMapper accountMapper;

    @Override
    public Mono<AccountDto> getByLogin(String login) {
        return accountRepository.findByLogin(login)
                .switchIfEmpty(Mono.error(new AccountNotFoundException("Аккаунт не найден: " + login)))
                .map(account -> accountMapper.toDto(account));
    }

    @Override
    @Transactional
    public Mono<AccountDto> updateByLogin(String login, AccountUpdateRq updateRq) {
        return accountRepository.findByLogin(login)
                .switchIfEmpty(Mono.error(new AccountNotFoundException("Аккаунт не найден: " + login)))
                .flatMap(account -> {
                    account.setFirstName(updateRq.getFirstName());
                    account.setLastName(updateRq.getLastName());
                    account.setBirthDate(updateRq.getBirthday());
                    return accountRepository.save(account);
                }).map(account -> accountMapper.toDto(account));
    }

    @Override
    public Flux<AccountShortDto> getOtherAccounts(String login) {
        return accountRepository.findAllByLoginNot(login).map(account -> accountMapper.toShortDto(account));
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
        return Mono.zip(
                executeDecrement(srcLogin, amount),
                executeIncrement(dstLogin, amount)
        ).then();
    }

    private Mono<Void> executeIncrement(String login, BigDecimal amount){
        return accountRepository.incrementBalance(login, amount)
                .flatMap(result -> {
                    if (result == 0) return Mono.error(new AccountNotFoundException(login));
                    return Mono.empty();
                });
    }

    private Mono<Void> executeDecrement(String login, BigDecimal amount){
        return accountRepository.decrementBalance(login, amount)
                .flatMap(result -> accountRepository.findByLogin(login)
                        .switchIfEmpty(Mono.error(new AccountNotFoundException(login)))
                        .flatMap(a -> Mono.error(new InsufficientFundsException(
                                "Недостаточно средств на счёте " + login)))
                );
    }
}
