package com.nhnacademy.recommendation.exception;

public class RequiredValueException extends RuntimeException {
    public RequiredValueException(String type) {
        super("%s는 필수 값입니다.".formatted(type));
    }
}
