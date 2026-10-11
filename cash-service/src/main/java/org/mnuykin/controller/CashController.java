package org.mnuykin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mnuykin.dto.AmountRequest;
import org.mnuykin.dto.CashResponse;
import org.mnuykin.service.CashService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/cash")
@RequiredArgsConstructor
public class CashController {
    private final CashService cashService;

    @PostMapping("/deposit")
    public Mono<CashResponse> deposit(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody AmountRequest request) {
        return cashService.deposit(loginFrom(jwt), request.amount());
    }

    @PostMapping("/withdraw")
    public Mono<CashResponse> withdraw(@AuthenticationPrincipal Jwt jwt,
                                       @Valid @RequestBody AmountRequest request) {
        return cashService.withdraw(loginFrom(jwt), request.amount());
    }

    private String loginFrom(Jwt jwt) {
        String preferred = jwt.getClaimAsString("preferred_username");
        return preferred != null ? preferred : jwt.getSubject();
    }
}