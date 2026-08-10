package com.nhnacademy.recommendation.dto.welcomeBriefing;

import java.util.List;

public record WelcomeBriefingContext(
        RoomInfo room,
        IndoorEnvironmentAnalysis indoorEnvironmentAnalysis,
        CurrentWeatherSnapshot currentWeather,
        TodayWeatherOutlook todayWeatherOutlook,
        List<DeviceStatus> devices
) {
}
