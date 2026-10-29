package ru.yandex.practicum.mybankfront.controller;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import ru.yandex.practicum.mybankfront.config.SecurityConfig;
import ru.yandex.practicum.mybankfront.controller.dto.AccountBriefDto;
import ru.yandex.practicum.mybankfront.controller.dto.AccountDto;
import ru.yandex.practicum.mybankfront.controller.dto.AccountShortView;
import ru.yandex.practicum.mybankfront.controller.dto.AccountUpdateRq;
import ru.yandex.practicum.mybankfront.controller.dto.CashRs;
import ru.yandex.practicum.mybankfront.controller.dto.TransferRs;
import ru.yandex.practicum.mybankfront.service.AccountsClient;
import ru.yandex.practicum.mybankfront.service.CashClient;
import ru.yandex.practicum.mybankfront.service.TransferClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(MainController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.security.oauth2.client.provider.keycloak.issuer-uri=",
        "spring.security.oauth2.client.provider.keycloak.authorization-uri=http://localhost:8090/auth",
        "spring.security.oauth2.client.provider.keycloak.token-uri=http://localhost:8090/token",
        "spring.security.oauth2.client.provider.keycloak.jwk-set-uri=http://localhost:8090/certs",
        "spring.security.oauth2.client.provider.keycloak.user-info-uri=http://localhost:8090/userinfo",
        "spring.security.oauth2.client.provider.keycloak.user-name-attribute=preferred_username",
        "logging.config=classpath:logback-test.xml",
})
class MainControllerTest {

    private static final AccountDto ME = new AccountDto(
            "login1", "Федя", "Сумкин",
            LocalDate.of(1990, 1, 1), new BigDecimal("1000.00"));

    private static final List<AccountBriefDto> OTHERS = List.of(
            new AccountBriefDto("login2", "Пётр", "Петров"),
            new AccountBriefDto("login3", "Дмитрий", "Петров"));

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AccountsClient accountsClient;
    @MockitoBean
    CashClient cashClient;
    @MockitoBean
    TransferClient transferClient;
    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    private void stubModel() {
        when(accountsClient.getMe()).thenReturn(ME);
        when(accountsClient.getOthers()).thenReturn(OTHERS);
    }

    private static HttpClientErrorException badRequest(String jsonBody) {
        return HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                HttpHeaders.EMPTY,
                jsonBody.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
    }


    @Nested
    class GetRoutes {

        @Test
        @WithMockUser
        void index_shouldRedirectToAccount() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/account"));
        }

        @Test
        @WithMockUser
        void getAccount_shouldRenderMainWithModel() throws Exception {
            stubModel();

            mockMvc.perform(get("/account"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("main"))
                    .andExpect(model().attribute("name", "Федя Сумкин"))
                    .andExpect(model().attribute("birthdate", "1990-01-01"))
                    .andExpect(model().attribute("sum", new BigDecimal("1000.00")))
                    .andExpect(model().attribute("accounts", List.of(
                            new AccountShortView("login2", "Пётр Петров"),
                            new AccountShortView("login3", "Дмитрий Петров"))));

            verify(accountsClient).getMe();
            verify(accountsClient).getOthers();
        }
    }

    @Nested
    class EditAccount {
        @Test
        @WithMockUser
        void editAccount_shouldParseAndUpdate() throws Exception {
            when(accountsClient.updateMe(any())).thenReturn(ME);
            stubModel();

            mockMvc.perform(post("/account")
                            .param("name", "Сумкин Фёдор")
                            .param("birthdate", "1990-01-01"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("main"))
                    .andExpect(model().attribute("info", "Данные сохранены"));

            ArgumentCaptor<AccountUpdateRq> captor = ArgumentCaptor.forClass(AccountUpdateRq.class);
            verify(accountsClient).updateMe(captor.capture());
            AccountUpdateRq sent = captor.getValue();
            assertThat(sent.getFirstName()).isEqualTo("Сумкин");
            assertThat(sent.getLastName()).isEqualTo("Фёдор");
            assertThat(sent.getBirthday()).isEqualTo(LocalDate.of(1990, 1, 1));
        }

        @Test
        @WithMockUser
        void editAccount_shouldShowError_whenClientFails() throws Exception {
            when(accountsClient.updateMe(any()))
                    .thenThrow(badRequest("{\"error\":\"Возраст должен быть не менее 18 лет\"}"));
            stubModel();

            mockMvc.perform(post("/account")
                            .param("name", "Сумкин Фёдор")
                            .param("birthdate", "2015-01-01"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("main"))
                    .andExpect(model().attribute("errors",
                            List.of("Возраст должен быть не менее 18 лет")));
        }

        @Test
        @WithMockUser
        void editAccount_shouldFallback_whenNoErrorField() throws Exception {
            when(accountsClient.updateMe(any()))
                    .thenThrow(HttpClientErrorException.create(
                            HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                            HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8));
            stubModel();

            mockMvc.perform(post("/account")
                            .param("name", "Сумкин Фёдор")
                            .param("birthdate", "1990-01-01"))
                    .andExpect(model().attribute("errors",
                            List.of("HTTP 500: Internal Server Error")));
        }
    }

    @Nested
    class Cash {

        @Test
        @WithMockUser
        void deposit_shouldCallDeposit_whenActionIsPut() throws Exception {
            when(cashClient.deposit(any())).thenReturn(
                    new CashRs("login1", new BigDecimal("1100"), "DEPOSIT"));
            stubModel();

            mockMvc.perform(post("/cash")
                            .param("value", "100")
                            .param("action", "PUT"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("main"))
                    .andExpect(model().attribute("info", "Счёт пополнен на 100 руб."));

            verify(cashClient).deposit(new BigDecimal("100"));
            verify(cashClient, never()).withdraw(any());
        }

        @Test
        @WithMockUser
        void withdraw_shouldCallWithdraw_whenActionIsGet() throws Exception {
            when(cashClient.withdraw(any())).thenReturn(
                    new CashRs("login1", new BigDecimal("900"), "WITHDRAW"));
            stubModel();

            mockMvc.perform(post("/cash")
                            .param("value", "100")
                            .param("action", "GET"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("info", "Со счёта снято 100 руб."));

            verify(cashClient).withdraw(new BigDecimal("100"));
            verify(cashClient, never()).deposit(any());
        }

        @Test
        @WithMockUser
        void withdraw_shouldShowError_whenInsufficientFunds() throws Exception {
            when(cashClient.withdraw(any()))
                    .thenThrow(badRequest("{\"error\":\"Недостаточно средств на счёте login1\"}"));
            stubModel();

            mockMvc.perform(post("/cash")
                            .param("value", "99999")
                            .param("action", "GET"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("errors",
                            List.of("Недостаточно средств на счёте login1")));
        }
    }

    // ================================================================= /transfer POST

    @Nested
    class Transfer {

        @Test
        @WithMockUser
        void transfer_shouldCallTransferAndShowInfo() throws Exception {
            when(transferClient.transfer(eq("login2"), any())).thenReturn(
                    new TransferRs("login1", "login2",
                            new BigDecimal("100"), new BigDecimal("900"), "TRANSFER"));
            stubModel();

            mockMvc.perform(post("/transfer")
                            .param("value", "100")
                            .param("login", "login2"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("info",
                            "Перевод 100 руб. на счёт login2 выполнен"));

            verify(transferClient).transfer("login2", new BigDecimal("100"));
        }

        @Test
        @WithMockUser
        void transfer_shouldShowError_whenInsufficientFunds() throws Exception {
            when(transferClient.transfer(any(), any()))
                    .thenThrow(badRequest("{\"error\":\"Недостаточно средств на счёте login1\"}"));
            stubModel();

            mockMvc.perform(post("/transfer")
                            .param("value", "99999")
                            .param("login", "login2"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("errors",
                            List.of("Недостаточно средств на счёте login1")));
        }
    }
}