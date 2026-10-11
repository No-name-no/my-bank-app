package org.mnuykin.dto;

import java.math.BigDecimal;

public record CashResponse(
        String login,
        BigDecimal balance,
        String operation
) {
}