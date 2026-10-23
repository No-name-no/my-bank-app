package org.mnuykin;

import org.junit.jupiter.api.Test;
import org.mnuykin.config.SecurityConfig;
import org.mnuykin.controller.TransferController;
import org.mnuykin.dto.TransferRs;
import org.mnuykin.exception.GlobalExceptionHandler;
import org.mnuykin.service.TransferService;
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

@WebFluxTest(controllers = TransferController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false"
})
class TransferControllerTest {

    @Autowired WebTestClient webTestClient;

    @MockitoBean TransferService transferService;

    @MockitoBean
    protected ReactiveJwtDecoder reactiveJwtDecoder;

    @Test
    void transfer_shouldReturn200() {
        when(transferService.transfer(any(), any())).thenReturn(Mono.just(
                new TransferRs("login1", "login2",
                        new BigDecimal("100"), new BigDecimal("900"), "TRANSFER")));

        webTestClient.mutateWith(mockJwt()
                        .jwt(j -> j.claim("preferred_username", "login1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("transfer-service_TRANSFER_WRITE")))
                .post().uri("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"recipientLogin\":\"login2\",\"amount\":100}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.senderLogin").isEqualTo("login1")
                .jsonPath("$.recipientLogin").isEqualTo("login2")
                .jsonPath("$.operation").isEqualTo("TRANSFER");
    }

    @Test
    void transfer_shouldReturn403WithoutRole() {
        webTestClient.mutateWith(mockJwt()
                        .jwt(j -> j.claim("preferred_username", "login1")))
                .post().uri("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"recipientLogin\":\"login2\",\"amount\":100}")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void transfer_shouldReturn400OnInvalidBody() {
        webTestClient.mutateWith(mockJwt()
                        .jwt(j -> j.claim("preferred_username", "login1"))
                        .authorities(new SimpleGrantedAuthority("transfer-service_TRANSFER_WRITE")))
                .post().uri("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"recipientLogin\":\"\",\"amount\":-5}")
                .exchange()
                .expectStatus().isBadRequest();
    }
}