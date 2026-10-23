package ru.yandex.practicum.mybankfront.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.mybankfront.controller.dto.TransferRq;
import ru.yandex.practicum.mybankfront.controller.dto.TransferRs;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class TransferClient {

    private final RestClient gatewayRestClient;

    public TransferRs transfer(String recipientLogin, BigDecimal amount) {
        return gatewayRestClient.post()
                .uri("/transfer")
                .body(new TransferRq(recipientLogin, amount))
                .retrieve()
                .body(TransferRs.class);
    }
}