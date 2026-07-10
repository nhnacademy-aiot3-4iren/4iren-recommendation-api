package com.nhnacademy.recommendation.dto.llm;

import java.util.List;

public record KmaForecastWeatherResponseDto(
        String requestedRegionName,
        String regionName,
        Integer nx,
        Integer ny,
        String baseDateTime,
        List<Forecast> forecasts
) {
    public record Forecast(
            String forecastDateTime,
            String sky,
            String precipitationType,
            String precipitationAmount,
            String precipitationProbability,
            String temperature,
            String humidity,
            String windDirection,
            String windSpeed,
            String eastWestWindComponent,
            String northSouthWindComponent,
            String lightning
    ) {
    }
}
