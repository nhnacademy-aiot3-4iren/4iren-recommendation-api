package com.nhnacademy.recommendation.dto.welcomeBriefing;

import java.util.List;

public record TodayWeatherOutlook(
        boolean rainExpected,
        boolean rainPossible,
        boolean strongWindExpected,
        boolean highHumidityExpected,
        String hottestTime,
        String ventilationBestTime,
        List<String> cautions
) {
}
