package org.mnuykin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRq {
    @NotNull
    private TranKind kind;
    @NotNull
    private BigDecimal amount;
    @NotNull
    private String srcLogin;
    private String dstLogin;
}