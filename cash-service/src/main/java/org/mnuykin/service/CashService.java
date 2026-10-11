package org.mnuykin.service;

import org.mnuykin.dto.CashResponse;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface CashService {
    Mono<CashResponse> deposit(String login, BigDecimal amount);
    Mono<CashResponse> withdraw(String login, BigDecimal amount);
}