package com.nhnacademy.recommendation.exception;

public class ModelServingException extends RuntimeException {

    public ModelServingException(String message) {
        super(message);
    }

    public ModelServingException(String message, Throwable cause) {
        super(message, cause);
    }
}
