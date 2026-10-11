package org.mnuykin;

import org.junit.jupiter.api.Test;
import org.mnuykin.config.SecurityConfig;
import org.mnuykin.controller.InternalAccountController;
import org.mnuykin.exception.GlobalExceptionHandler;
import org.mnuykin.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(controllers = InternalAccountController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class InternalAccountControllerTest {

    @Autowired WebTestClient webTestClient;

    @MockitoBean AccountService accountService;

    @Test
    void transaction_shouldReturn200_forServiceToken() {
        when(accountService.execTran(any())).thenReturn(Mono.empty());

        webTestClient.mutateWith(mockJwt()
                        .jwt(j -> j.claim("azp", "cash-service"))
                        .authorities(
                                new SimpleGrantedAuthority("ROLE_SERVICE"),
                                new SimpleGrantedAuthority("accounts-service_ACCOUNTS_WRITE")))
                .post().uri("/accounts/internal/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"kind":"CASH_DEPOSIT","amount":100.00,"srcLogin":"login1"}
                        """)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void transaction_shouldReturn403_forUserToken() {
        webTestClient.mutateWith(mockJwt()
                        .jwt(j -> j.claim("preferred_username", "login1"))
                        .authorities(
                                new SimpleGrantedAuthority("ROLE_USER"),
                                new SimpleGrantedAuthority("accounts-service_ACCOUNTS_WRITE")))
                .post().uri("/accounts/internal/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"kind":"CASH_DEPOSIT","amount":100.00,"srcLogin":"login1"}
                        """)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void transaction_shouldReturn400_onMissingDstForTransfer() {
        webTestClient.mutateWith(mockJwt()
                        .jwt(j -> j.claim("azp", "transfer-service"))
                        .authorities(
                                new SimpleGrantedAuthority("ROLE_SERVICE"),
                                new SimpleGrantedAuthority("accounts-service_ACCOUNTS_WRITE")))
                .post().uri("/accounts/internal/transaction")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"kind":"TRANSFER","amount":100.00,"srcLogin":"login1"}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }
}