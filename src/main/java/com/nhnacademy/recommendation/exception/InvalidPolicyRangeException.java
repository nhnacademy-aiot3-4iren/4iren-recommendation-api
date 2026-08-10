package com.nhnacademy.recommendation.exception;

public class InvalidPolicyRangeException extends RuntimeException {

    public InvalidPolicyRangeException(String lowerType, Number lowerValue, String upperType, Number upperValue) {
        super("%s 값은 %s 값보다 클 수 없습니다. %s=%s, %s=%s"
                .formatted(lowerType, upperType, lowerType, lowerValue, upperType, upperValue));
    }
}
