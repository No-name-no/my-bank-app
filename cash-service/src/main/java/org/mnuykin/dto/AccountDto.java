package org.mnuykin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {
    private String login;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private BigDecimal balance;
}