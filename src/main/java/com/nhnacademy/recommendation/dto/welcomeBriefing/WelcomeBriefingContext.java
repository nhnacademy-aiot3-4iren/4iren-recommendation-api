package com.nhnacademy.recommendation.dto.welcomeBriefing;

import java.util.List;

public record WelcomeBriefingContext(
        RoomInfo room,
        IndoorEnvironmentPolicy policy,
        CurrentIndoorSnapshot currentIndoor,
        CurrentWeatherSnapshot currentWeather,
        TodayWeatherOutlook todayWeatherOutlook,
        YesterdayPatternSummary yesterdayPattern,
        List<DeviceStatus> devices,
        List<String> detectedRisks
) {
}
