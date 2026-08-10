package com.nhnacademy.recommendation.exception;

public class ProbabilityRangeException extends RuntimeException {

    public ProbabilityRangeException(int value, String type) {
        super("%s 값은 0 이상 100 이하이어야 합니다. value=%d".formatted(type, value));
    }
}
