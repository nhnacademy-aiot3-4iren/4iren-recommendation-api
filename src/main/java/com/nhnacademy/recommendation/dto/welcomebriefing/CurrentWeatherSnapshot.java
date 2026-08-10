package com.nhnacademy.recommendation.dto.welcomebriefing;

import java.time.LocalDateTime;

public record CurrentWeatherSnapshot(
        String regionName,
        LocalDateTime measuredAt,
        String temperature,
        String humidity,
        String precipitationType,
        String precipitationAmount,
        String windSpeed
) {
}
