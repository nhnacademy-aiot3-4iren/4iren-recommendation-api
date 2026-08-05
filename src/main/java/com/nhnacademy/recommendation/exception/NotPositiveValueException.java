package com.nhnacademy.recommendation.exception;

public class NotPositiveValueException extends RuntimeException {
    public NotPositiveValueException(Long value, String type) {
        super("%s 값은 양수여야 합니다. value=%d".formatted(type, value));
    }
}
