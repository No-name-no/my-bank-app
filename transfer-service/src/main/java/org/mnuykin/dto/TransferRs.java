package org.mnuykin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class TransferRs {
    private String senderLogin;
    private String recipientLogin;
    private BigDecimal amount;
    private BigDecimal senderBalance;
    private String operation;
}