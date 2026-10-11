package org.mnuykin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mnuykin.dto.TransferRq;
import org.mnuykin.dto.TransferRs;
import org.mnuykin.service.TransferService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/transfer")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @PreAuthorize("hasRole('USER') and hasAuthority('transfer-service_TRANSFER_WRITE')")
    public Mono<TransferRs> transfer(@AuthenticationPrincipal Jwt jwt,
                                     @Valid @RequestBody TransferRq rq) {
        String login = loginFrom(jwt);
        return transferService.transfer(login, rq);
    }

    private String loginFrom(Jwt jwt) {
        String preferred = jwt.getClaimAsString("preferred_username");
        return preferred != null ? preferred : jwt.getSubject();
    }
}