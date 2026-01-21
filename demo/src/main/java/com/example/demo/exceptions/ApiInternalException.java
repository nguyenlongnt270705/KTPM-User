package com.example.demo.exceptions;

import lombok.Getter;

import java.io.Serial;

@Getter
public class ApiInternalException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ErrorMessage errorMessage;

    public ApiInternalException(ErrorMessage errorMessage) {
        super(errorMessage.getMessage());
        this.errorMessage = errorMessage;
    }

    public ApiInternalException(ErrorMessage errorMessage, String message) {
        super(message);
        this.errorMessage = errorMessage;
    }

    public static ApiInternalException error(ErrorMessage errorMessage) {
        return new ApiInternalException(errorMessage);
    }
}
