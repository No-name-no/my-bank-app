package org.mnuykin;

import org.junit.jupiter.api.Test;
import org.mnuykin.config.SecurityConfig;
import org.mnuykin.controller.CashController;
import org.mnuykin.dto.CashRs;
import org.mnuykin.exception.GlobalExceptionHandler;
import org.mnuykin.service.CashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(controllers = CashController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "logging.config=classpath:logback-test.xml"
})
class CashControllerTest {

    @Autowired WebTestClient webTestClient;

    @MockitoBean CashService cashService;

    @MockitoBean
    protected ReactiveJwtDecoder reactiveJwtDecoder;

    @Test
    void deposit_shouldReturn200() {
        when(cashService.deposit(any(), any())).thenReturn(Mono.just(
                new CashRs("login1", new BigDecimal("1100"), "DEPOSIT")));

        webTestClient.mutateWith(mockJwt()
                        .jwt(j -> j.claim("preferred_username", "login1"))
                        .authorities(new SimpleGrantedAuthority("cash-service_CASH_WRITE")))
                .post().uri("/cash/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":100.00}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.operation").isEqualTo("DEPOSIT")
                .jsonPath("$.login").isEqualTo("login1");
    }

    @Test
    void deposit_shouldReturn403WithoutRole() {
        webTestClient.mutateWith(mockJwt()
                        .jwt(j -> j.claim("preferred_username", "login1")))
                .post().uri("/cash/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":100.00}")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void deposit_shouldReturn400OnInvalidAmount() {
        webTestClient.mutateWith(mockJwt()
                        .jwt(j -> j.claim("preferred_username", "login1"))
                        .authorities(new SimpleGrantedAuthority("cash-service_CASH_WRITE")))
                .post().uri("/cash/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"amount\":-5}")
                .exchange()
                .expectStatus().isBadRequest();
    }
}