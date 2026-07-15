package com.nhnacademy.recommendation.exception;

public class InvalidMessageException extends RuntimeException {
    public InvalidMessageException() {
        super("메시지는 null이거나 빈 값일 수 없습니다.");
    }
}
