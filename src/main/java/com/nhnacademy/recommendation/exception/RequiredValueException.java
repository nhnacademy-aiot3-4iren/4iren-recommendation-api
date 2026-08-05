package com.nhnacademy.recommendation.exception;

public class RequiredValueException extends RuntimeException {
    public RequiredValueException(String type) {
        super(type+"이 null입니다.");
    }
}
