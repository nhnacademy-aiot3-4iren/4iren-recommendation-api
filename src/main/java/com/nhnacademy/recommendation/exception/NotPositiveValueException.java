package com.nhnacademy.recommendation.exception;

public class NotPositiveValueException extends RuntimeException {
    public NotPositiveValueException(Long id, String message) {
        super("%s 값은 양수여야 합니다. %s: %d".formatted(message, message, id));
    }
}
