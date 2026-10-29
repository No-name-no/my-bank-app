package org.mnuykin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ErrorRs {
    private String code;
    private String message;
}