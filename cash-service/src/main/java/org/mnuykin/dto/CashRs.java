package org.mnuykin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CashRs {
    private String login;
    private BigDecimal balance;
    private String operation;
}