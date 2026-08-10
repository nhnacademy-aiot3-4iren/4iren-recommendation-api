package com.nhnacademy.recommendation.service.core;

import com.nhnacademy.recommendation.exception.NotPositiveValueException;
import com.nhnacademy.recommendation.exception.RequiredValueException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CoreRequestValidator {

    public static void requirePositive(Long id, String type) {
        if (id == null) {
            throw new RequiredValueException(type);
        }
        if (id <= 0) {
            throw new NotPositiveValueException(id, type);
        }
    }

    public static void requireNonNull(Object value, String type) {
        if (value == null) {
            throw new RequiredValueException(type);
        }
    }

    public static void requireText(String value, String type) {
        if (value == null || value.isBlank()) {
            throw new RequiredValueException(type);
        }
    }
}
