package org.mnuykin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mnuykin.dto.AmountRq;
import org.mnuykin.dto.CashRs;
import org.mnuykin.service.CashService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/cash")
public class CashController {

    private final CashService cashService;

    @PostMapping("/deposit")
    public Mono<CashRs> deposit(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AmountRq rq) {
        return cashService.deposit(loginFrom(jwt), rq.getAmount());
    }

    @PostMapping("/withdraw")
    public Mono<CashRs> withdraw(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AmountRq rq) {
        return cashService.withdraw(loginFrom(jwt), rq.getAmount());
    }

    private String loginFrom(Jwt jwt) {
        String preferred = jwt.getClaimAsString("preferred_username");
        return preferred != null ? preferred : jwt.getSubject();
    }
}