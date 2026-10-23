package ru.yandex.practicum.mybankfront.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.mybankfront.controller.dto.CashRq;
import ru.yandex.practicum.mybankfront.controller.dto.CashRs;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class CashClient {

    private final RestClient gatewayRestClient;

    public CashRs deposit(BigDecimal amount) {
        return gatewayRestClient.post()
                .uri("/cash/deposit")
                .body(new CashRq(amount))
                .retrieve()
                .body(CashRs.class);
    }

    public CashRs withdraw(BigDecimal amount) {
        return gatewayRestClient.post()
                .uri("/cash/withdraw")
                .body(new CashRq(amount))
                .retrieve()
                .body(CashRs.class);
    }
}