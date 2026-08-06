package com.nhnacademy.recommendation.dto.welcomeBriefing;

public record TeamIndoorPolicyUpdateRequest(
        Integer co2WarningPpm,
        Integer co2DangerPpm,
        Double humidityLowPercent,
        Double humidityHighPercent,
        Double temperatureLowCelsius,
        Double temperatureHighCelsius,
        Double pm25Warning,
        Double pm10Warning,
        Double strongWindSpeed,
        Integer staleSensorMinutes,
        Integer rainPossibleProbability,
        Integer rainExpectedProbability
) {
    public TeamIndoorPolicyOverride toOverride(Long teamId) {
        return new TeamIndoorPolicyOverride(
                teamId,
                co2WarningPpm,
                co2DangerPpm,
                humidityLowPercent,
                humidityHighPercent,
                temperatureLowCelsius,
                temperatureHighCelsius,
                pm25Warning,
                pm10Warning,
                strongWindSpeed,
                staleSensorMinutes,
                rainPossibleProbability,
                rainExpectedProbability
        );
    }
}
