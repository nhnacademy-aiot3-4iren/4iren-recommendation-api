package com.nhnacademy.recommendation.dto.kma;

public record KmaCurrentWeatherResponseDto(
        String requestedRegionName,
        String regionName,
        Integer nx,
        Integer ny,
        String baseDateTime,
        String temperature,
        String precipitationType,
        String precipitationAmount,
        String humidity,
        String windDirection,
        String windSpeed,
        String eastWestWindComponent,
        String northSouthWindComponent
) {
}
