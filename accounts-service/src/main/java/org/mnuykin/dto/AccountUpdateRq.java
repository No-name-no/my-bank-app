package org.mnuykin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountUpdateRq {
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotNull
    @Past
    private LocalDate birthday;
}
