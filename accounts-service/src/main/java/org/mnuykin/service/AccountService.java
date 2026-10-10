package org.mnuykin.service;

import org.mnuykin.dto.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AccountService {
    Mono<AccountDto> getByLogin(String login);
    Mono<AccountDto> updateByLogin(String login, AccountUpdateRq updateRq);
    Flux<AccountShortDto> getOtherAccounts(String login);
    Mono<Void> execTran(TransactionRq transactionRq);
}