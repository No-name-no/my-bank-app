package org.mnuykin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mnuykin.dto.AccountDto;
import org.mnuykin.dto.TransactionRq;
import org.mnuykin.service.AccountService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/accounts/internal")
public class InternalAccountController {

    private final AccountService accountService;

    @PostMapping("/transaction")
    @PreAuthorize("hasRole('SERVICE') and hasAuthority('accounts-service_ACCOUNTS_WRITE')")
    public Mono<Void> transaction(@Valid @RequestBody TransactionRq transactionRq){
        return accountService.execTran(transactionRq);
    }

    @GetMapping("/{login}")
    @PreAuthorize("hasRole('SERVICE') and hasAuthority('accounts-service_ACCOUNTS_WRITE')")
    public Mono<AccountDto> getByLogin(@PathVariable String login) {
        return accountService.getByLogin(login);
    }
}
