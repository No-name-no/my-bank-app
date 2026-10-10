package org.mnuykin.repository;
import org.mnuykin.entity.Account;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface AccountRepository extends ReactiveCrudRepository<Account, Long> {

    Mono<Account> findByLogin(String login);

    Flux<Account> findAllByLoginNot(String login);

    @Modifying
    @Query("""
            UPDATE accounts.accounts
            SET balance = balance + :amount
            WHERE login = :login
            """)
    Mono<Integer> incrementBalance(String login, BigDecimal amount);

    @Modifying
    @Query("""
            UPDATE accounts.accounts
            SET balance = balance - :amount
            WHERE login = :login
            """)
    Mono<Integer> decrementBalance(String login, BigDecimal amount);
}