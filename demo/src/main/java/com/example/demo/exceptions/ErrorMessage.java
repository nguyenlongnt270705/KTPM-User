package com.example.demo.exceptions;

import lombok.Getter;

@Getter
public enum ErrorMessage {
    SUCCESS("00", "Success"),
    USER_NOT_FOUND("USER_NOT_FOUND", "User not found"),
    VALIDATION_ERROR("400", "Validation error"),
    CURRENT_PASSWORD_INVALID("CURRENT_PASSWORD_INVALID", "Current password invalid"),
    ACCESS_DENIED("403", "Access denied"),

    ;
    private final String code;
    private final String message;

    ErrorMessage(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
