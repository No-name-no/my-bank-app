package ru.yandex.practicum.mybankfront.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountUpdateRq {
    private String firstName;
    private String lastName;
    private LocalDate birthday;
}