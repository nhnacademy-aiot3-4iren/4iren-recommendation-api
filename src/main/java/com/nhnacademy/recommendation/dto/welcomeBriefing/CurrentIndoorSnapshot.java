package com.nhnacademy.recommendation.dto.welcomeBriefing;

import java.time.LocalDateTime;

public record CurrentIndoorSnapshot(
        LocalDateTime measuredAt,
        Double temperatureCelsius,
        Double humidityPercent,
        Integer co2Ppm,
        Double pm25,
        Double pm10
) {
}
