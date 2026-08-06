package com.nhnacademy.recommendation.dto.welcomeBriefing;

public record IndoorEnvironmentPolicy(
        int co2WarningPpm,
        int co2DangerPpm,
        double humidityLowPercent,
        double humidityHighPercent,
        double temperatureLowCelsius,
        double temperatureHighCelsius,
        double pm25Warning,
        double pm10Warning,
        double strongWindSpeed,
        int staleSensorMinutes,
        int rainPossibleProbability,
        int rainExpectedProbability
) {
    public static IndoorEnvironmentPolicy defaults() {
        return new IndoorEnvironmentPolicy(
                1000,
                1500,
                30.0,
                60.0,
                20.0,
                26.0,
                35.0,
                75.0,
                8.0,
                15,
                30,
                60
        );
    }
}
