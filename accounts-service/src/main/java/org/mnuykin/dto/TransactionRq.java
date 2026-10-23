package org.mnuykin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
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
    @NotBlank
    private String srcLogin;
    private String dstLogin;

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
