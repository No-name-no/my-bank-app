package ru.yandex.practicum.mybankfront.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferRs {
    private String senderLogin;
    private String recipientLogin;
    private BigDecimal amount;
    private BigDecimal senderBalance;
    private String operation;
}