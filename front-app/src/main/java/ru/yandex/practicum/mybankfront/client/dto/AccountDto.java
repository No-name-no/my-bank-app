package ru.yandex.practicum.mybankfront.client.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountDto(
        String login,
        String firstName,
        String lastName,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate birthDate,
        BigDecimal balance
) {}