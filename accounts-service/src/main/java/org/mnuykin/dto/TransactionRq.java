package org.mnuykin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class TransactionRq {
    @NotNull
    private TranKind kind;
    @NotNull
    private BigDecimal amount;
    @NotBlank
    private String srcLogin;

    private String dstLogin;

    @AssertTrue(message = "dstLogin is not be blank")
    private boolean isDstLoginValid() {
        if (kind != TranKind.TRANSFER) {
            return true;
        }
        return dstLogin != null && !dstLogin.trim().isEmpty();
    }
}
