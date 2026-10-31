package org.mnuykin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mnuykin.dto.AccountDto;
import org.mnuykin.dto.ErrorRs;
import org.mnuykin.dto.TransactionRq;
import org.mnuykin.exception.AccountNotFoundException;
import org.mnuykin.exception.InsufficientFundsException;
import org.mnuykin.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/internal/accounts")
public class InternalAccountController {

    private final AccountService accountService;

    @PostMapping("/transaction")
    @PreAuthorize("hasRole('SERVICE') and hasAuthority('accounts-service_ACCOUNTS_WRITE')")
    public Mono<Void> transaction(@Valid @RequestBody TransactionRq transactionRq){
        log.info("Transaction request: {}", transactionRq.toString());
        return accountService.execTran(transactionRq);
    }

    @GetMapping("/{login}")
    @PreAuthorize("hasRole('SERVICE') and hasAuthority('accounts-service_ACCOUNTS_WRITE')")
    public Mono<AccountDto> getByLogin(@PathVariable String login) {
        log.info("Get account: login={}", login);
        return accountService.getByLogin(login);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorRs> notFound(AccountNotFoundException e) {
        return Mono.just(new ErrorRs("AccountNotFound", e.getMessage()));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorRs> insufficient(InsufficientFundsException e) {
        return Mono.just(new ErrorRs("InsufficientFunds", e.getMessage()));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorRs> validation(WebExchangeBindException e) {
        String msg = e.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Mono.just(new ErrorRs("Validation", msg));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorRs> illegal(IllegalArgumentException e) {
        return Mono.just(new ErrorRs("IllegalArgument", e.getMessage()));
    }
}
