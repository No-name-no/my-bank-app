package org.mnuykin.dto;

import jakarta.validation.constraints.*;
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
    @NotNull(message = "Сумма обязательна")
    @DecimalMin(value = "0.01", message = "Сумма должна быть больше нуля")
    private BigDecimal amount;
    @NotBlank
    private String srcLogin;
    private String dstLogin;
    @Size(max = 128, message = "RE - Ид. не может быть больше 128 символов")
    @NotBlank
    private String transactionId;

    @AssertTrue(message = "dstLogin must be set for TRANSFER")
    private boolean isDstLoginValid() {
        if (kind != TranKind.TRANSFER) {
            return true;
        }
        return dstLogin != null && !dstLogin.isBlank();
    }

    @AssertTrue(message = "srcLogin and dstLogin must be different")
    private boolean isSrcNotEqualToDst() {
        if (kind != TranKind.TRANSFER) {
            return true;
        }
        return srcLogin != null && !srcLogin.equals(dstLogin);
    }
}
