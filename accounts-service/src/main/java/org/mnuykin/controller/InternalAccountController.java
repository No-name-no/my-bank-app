package org.mnuykin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mnuykin.dto.TransactionRq;
import org.mnuykin.service.AccountService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/accounts/internal")
public class InternalAccountController {

    private final AccountService accountService;

    @PostMapping("/transaction")
    public Mono<Void> transaction(@Valid @RequestBody TransactionRq transactionRq){
        return accountService.execTran(transactionRq);
    }
}
