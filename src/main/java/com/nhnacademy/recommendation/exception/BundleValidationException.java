package com.nhnacademy.recommendation.exception;

public class BundleValidationException extends ModelServingException {

    public BundleValidationException(String message) {
        super(message);
    }

    public BundleValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
