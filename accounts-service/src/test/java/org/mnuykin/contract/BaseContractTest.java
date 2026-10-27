package org.mnuykin.contract;

import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.mnuykin.dto.AccountDto;
import org.mnuykin.dto.AccountShortDto;
import org.mnuykin.dto.AccountUpdateRq;
import org.mnuykin.dto.TransactionRq;
import org.mnuykin.exception.InsufficientFundsException;
import org.mnuykin.producer.NotificationProducer;
import org.mnuykin.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import=",
                "eureka.client.enabled=false",
                "spring.r2dbc.url=r2dbc:postgresql://localhost:1/fake",
                "spring.r2dbc.username=fake",
                "spring.r2dbc.password=fake",
                "spring.sql.init.mode=never",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
                "spring.autoconfigure.exclude=org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration",
                "logging.config=classpath:logback-test.xml"
        })
@org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
@ActiveProfiles("contract")
@Import(ContractJwtConfig.class)
public abstract class BaseContractTest {

    @Autowired
    WebTestClient webTestClient;

    @MockitoBean
    ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

    @MockitoBean
    NotificationProducer notificationProducer;

    @MockitoBean
    AccountService accountService;

    @BeforeEach
    void setup() {
        when(accountService.getByLogin("login1")).thenReturn(Mono.just(
                new AccountDto("login1", "Федя", "Сумкин",
                        LocalDate.of(1990, 1, 1), new BigDecimal("1000.00"))));

        when(accountService.updateByLogin(eq("login1"), any(AccountUpdateRq.class)))
                .thenAnswer(inv -> {
                    AccountUpdateRq rq = inv.getArgument(1);
                    if (rq.getBirthday() != null
                            && rq.getBirthday().isAfter(LocalDate.now().minusYears(18))) {
                        return Mono.error(new IllegalArgumentException(
                                "Возраст должен быть не менее 18 лет"));
                    }
                    return Mono.just(new AccountDto("login1", "Фёдор", "Сумкин",
                            LocalDate.of(1990, 1, 1), new BigDecimal("1000.00")));
                });

        when(accountService.getOtherAccounts("login1")).thenReturn(Flux.just(
                new AccountShortDto("login2", "Пётр", "Петров"),
                new AccountShortDto("login3", "Дмитрий", "Петров")));

        when(accountService.execTran(any(TransactionRq.class)))
                .thenAnswer(inv -> {
                    TransactionRq rq = inv.getArgument(0);
                    if (rq.getAmount() != null
                            && rq.getAmount().compareTo(new BigDecimal("10000")) > 0) {
                        return Mono.error(new InsufficientFundsException(
                                "Недостаточно средств на счёте " + rq.getSrcLogin()));
                    }
                    return Mono.empty();
                });

        RestAssuredWebTestClient.webTestClient(webTestClient);
    }

    /** JWT с ролями, достаточными для всех @PreAuthorize в контроллерах. */
    private static Jwt testJwt() {
        Instant now = Instant.now();
        return Jwt.withTokenValue("test-jwt")
                .header("alg", "none")
                .claim("sub", "test-user")
                .claim("preferred_username", "login1")
                .claim("realm_access", Map.of("roles", List.of("USER", "SERVICE")))
                .claim("resource_access", Map.of(
                        "accounts-service", Map.of("roles", List.of("ACCOUNTS_WRITE"))))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
    }
}