package org.mnuykin.service;

import org.mnuykin.dto.CashRs;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface CashService {
    Mono<CashRs> deposit(String login, BigDecimal amount);
    Mono<CashRs> withdraw(String login, BigDecimal amount);
}