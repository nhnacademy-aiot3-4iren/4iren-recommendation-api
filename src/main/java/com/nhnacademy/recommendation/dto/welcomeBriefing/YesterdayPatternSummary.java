package com.nhnacademy.recommendation.dto.welcomeBriefing;

import java.util.List;

public record YesterdayPatternSummary(
        List<Integer> highCo2Hours,
        List<Integer> highHumidityHours,
        Integer peakCo2Hour,
        Double sameHourCo2Difference,
        IndoorTrend trend
) {
}
