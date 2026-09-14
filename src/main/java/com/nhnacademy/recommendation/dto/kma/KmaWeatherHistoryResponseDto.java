package com.nhnacademy.recommendation.dto.kma;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record KmaWeatherHistoryResponseDto(
        String requestedRegionName,
        String regionName,
        LocalDate date,
        AnalysisPeriod analysisPeriod,
        Integer expectedHours,
        Integer availableHours,
        boolean dataSufficient,
        List<LocalDateTime> missingHours,
        List<WeatherSnapshot> snapshots
) {
    public record AnalysisPeriod(
            LocalTime start,
            LocalTime end
    ) {
    }

    public record WeatherSnapshot(
            LocalDateTime observedAt,
            Double temperature,
            Integer humidity,
            String precipitationType,
            Double precipitationAmount,
            Double windSpeed
    ) {
    }
}
