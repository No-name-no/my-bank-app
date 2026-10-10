package org.mnuykin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mnuykin.dto.AccountDto;
import org.mnuykin.dto.AccountShortDto;
import org.mnuykin.dto.AccountUpdateRq;
import org.mnuykin.service.AccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/me")
    public Mono<AccountDto> me(@AuthenticationPrincipal Jwt jwt){
        return accountService.getByLogin(loginFrom(jwt));
    }

    @PutMapping("/me")
    public Mono<AccountDto> updateMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AccountUpdateRq updateRq){
        return accountService.updateByLogin(loginFrom(jwt), updateRq);
    }

    @GetMapping
    public Flux<AccountShortDto> changeAccount(@AuthenticationPrincipal Jwt jwt){
        return accountService.getOtherAccounts(loginFrom(jwt));
    }

    private String loginFrom(Jwt jwt) {
        String preferred = jwt.getClaimAsString("preferred_username");
        return preferred != null ? preferred : jwt.getSubject();
    }
}