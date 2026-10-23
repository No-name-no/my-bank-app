package ru.yandex.practicum.mybankfront.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.mybankfront.controller.dto.AccountBriefDto;
import ru.yandex.practicum.mybankfront.controller.dto.AccountDto;
import ru.yandex.practicum.mybankfront.controller.dto.AccountUpdateRq;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AccountsClient {
    private final RestClient gatewayRestClient;

    public AccountDto getMe() {
        return gatewayRestClient.get()
                .uri("/accounts/me")
                .retrieve()
                .body(AccountDto.class);
    }

    public AccountDto updateMe(AccountUpdateRq rq) {
        return gatewayRestClient.put()
                .uri("/accounts/me")
                .body(rq)
                .retrieve()
                .body(AccountDto.class);
    }

    public List<AccountBriefDto> getOthers() {
        return gatewayRestClient.get()
                .uri("/accounts")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}