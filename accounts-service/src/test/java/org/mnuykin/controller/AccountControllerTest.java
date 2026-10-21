package org.mnuykin.controller;

import org.junit.jupiter.api.Test;
import org.mnuykin.dto.AccountDto;
import org.mnuykin.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

public class AccountControllerTest extends BaseControllerTest {
    @Autowired
    private AccountService accountService;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void me_shouldReturn200_forUser() {
        when(accountService.getByLogin(eq("login1"))).thenReturn(Mono.just(
                new AccountDto("login1", "Федя", "Сумкин",
                        LocalDate.of(1990, 1, 1), new BigDecimal("1000.00"))));

        webTestClient.mutateWith(mockJwt()
                        .jwt(j -> j.claim("preferred_username", "login1"))
                        .authorities(
                                new SimpleGrantedAuthority("ROLE_USER"),
                                new SimpleGrantedAuthority("accounts-service_ACCOUNTS_WRITE")))
                .get().uri("/accounts/me")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.login").isEqualTo("login1")
                .jsonPath("$.balance").isEqualTo(1000.00);
    }

    @Test
    void me_shouldReturn403_forServiceToken() {
        webTestClient.mutateWith(mockJwt()
                        .jwt(j -> j.claim("azp", "cash-service"))
                        .authorities(
                                new SimpleGrantedAuthority("ROLE_SERVICE"),
                                new SimpleGrantedAuthority("accounts-service_ACCOUNTS_WRITE")))
                .get().uri("/accounts/me")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void me_shouldReturn403_withoutClientRole() {
        webTestClient.mutateWith(mockJwt()
                        .jwt(j -> j.claim("preferred_username", "login1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER")))
                .get().uri("/accounts/me")
                .exchange()
                .expectStatus().isForbidden();
    }
}
