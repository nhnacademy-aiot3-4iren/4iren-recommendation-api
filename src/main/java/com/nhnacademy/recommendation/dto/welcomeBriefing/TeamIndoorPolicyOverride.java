package com.nhnacademy.recommendation.dto.welcomeBriefing;

public record TeamIndoorPolicyOverride(
        Long teamId,
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
    public IndoorEnvironmentPolicy applyTo(IndoorEnvironmentPolicy defaults) {
        return new IndoorEnvironmentPolicy(
                valueOrDefault(co2WarningPpm, defaults.co2WarningPpm()),
                valueOrDefault(co2DangerPpm, defaults.co2DangerPpm()),
                valueOrDefault(humidityLowPercent, defaults.humidityLowPercent()),
                valueOrDefault(humidityHighPercent, defaults.humidityHighPercent()),
                valueOrDefault(temperatureLowCelsius, defaults.temperatureLowCelsius()),
                valueOrDefault(temperatureHighCelsius, defaults.temperatureHighCelsius()),
                valueOrDefault(pm25Warning, defaults.pm25Warning()),
                valueOrDefault(pm10Warning, defaults.pm10Warning()),
                valueOrDefault(strongWindSpeed, defaults.strongWindSpeed()),
                valueOrDefault(staleSensorMinutes, defaults.staleSensorMinutes()),
                valueOrDefault(rainPossibleProbability, defaults.rainPossibleProbability()),
                valueOrDefault(rainExpectedProbability, defaults.rainExpectedProbability())
        );
    }

    private static int valueOrDefault(Integer value, int defaultValue) {
        return value != null ? value : defaultValue;
    }

    private static double valueOrDefault(Double value, double defaultValue) {
        return value != null ? value : defaultValue;
    }
}
