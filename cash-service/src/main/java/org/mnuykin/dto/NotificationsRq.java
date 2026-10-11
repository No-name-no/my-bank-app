package org.mnuykin.dto;

import jakarta.validation.constraints.NotBlank;

public record NotificationsRq(
        @NotBlank String login,
        @NotBlank String subject,
        @NotBlank String message
) {
}