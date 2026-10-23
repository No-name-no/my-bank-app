package ru.yandex.practicum.mybankfront.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferRq {
    private String recipientLogin;
    private BigDecimal amount;
}