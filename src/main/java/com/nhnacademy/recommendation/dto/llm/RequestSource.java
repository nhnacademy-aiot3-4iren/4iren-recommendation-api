package com.nhnacademy.recommendation.dto.llm;

import java.util.Locale;

public enum RequestSource {
    WEB,
    TELEGRAM;

    public static RequestSource from(String value) {
        if (value == null || value.isBlank()) {
            return WEB;
        }
        try {
            return RequestSource.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return WEB;
        }
    }
}
