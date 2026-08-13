package com.nhnacademy.recommendation.dto.welcomebriefing;

import java.util.List;

public record WelcomeBriefingContext(
        RoomInfo room,
        CurrentSensorSnapshot currentSensor,
        CurrentWeatherSnapshot currentWeather,
        TodayWeatherOutlook todayWeatherOutlook,
        List<DeviceStatus> devices,
        WelcomeBriefingMlRecommendation mlRecommendation
) {
}
