package ru.yandex.practicum.mybankfront.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccountShortView {
    private String login;
    private String name;
}