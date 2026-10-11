package org.mnuykin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationsRq {
    @NotBlank
    private String login;
    @NotBlank
    private String subject;
    @NotBlank
    private String message;
}