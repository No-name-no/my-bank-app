package org.mnuykin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountShortDto {
    private String login;
    private String firstName;
    private String lastName;
}