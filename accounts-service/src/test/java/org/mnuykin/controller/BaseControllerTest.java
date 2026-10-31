package org.mnuykin.controller;

import org.mnuykin.config.SecurityConfig;
import org.mnuykin.exception.GlobalExceptionHandler;
import org.mnuykin.service.AccountService;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebFluxTest({AccountController.class, InternalAccountController.class, SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "logging.config=classpath:logback-test.xml"
})
@AutoConfigureWebTestClient
public abstract class BaseControllerTest {
    @MockitoBean
    protected AccountService accountService;

    @MockitoBean
    protected ReactiveJwtDecoder reactiveJwtDecoder;
}