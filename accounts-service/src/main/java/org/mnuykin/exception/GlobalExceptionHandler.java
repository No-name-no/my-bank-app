package org.mnuykin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Map<String, String>> notFound(AccountNotFoundException e) {
        return Mono.just(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Map<String, String>> insufficient(InsufficientFundsException e) {
        return Mono.just(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Map<String, String>> validation(WebExchangeBindException e) {
        String msg = e.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Mono.just(Map.of("error", msg));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Map<String, String>> illegal(IllegalArgumentException e) {
        return Mono.just(Map.of("error", e.getMessage()));
    }
}