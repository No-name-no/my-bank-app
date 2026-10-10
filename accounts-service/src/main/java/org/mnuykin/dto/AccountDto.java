package org.mnuykin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountDto(
        String login,
        String firstName,
        String lastName,
        LocalDate birthDate,
        BigDecimal balance
) {}