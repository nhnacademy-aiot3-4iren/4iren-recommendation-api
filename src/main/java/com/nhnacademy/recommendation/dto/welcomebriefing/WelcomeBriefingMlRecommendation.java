package com.nhnacademy.recommendation.dto.welcomebriefing;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record WelcomeBriefingMlRecommendation(
        String schemaVersion,
        Context context,
        String recommendationType,
        List<RecommendedSchedule> recommendedSchedule
) {

    public record Context(
            LocalDate predictionDate,
            DayOfWeek weekday,
            Long roomId,
            String location,
            String timezone
    ) {
    }

    public record RecommendedSchedule(
            String deviceType,
            String action,
            LocalTime startTime,
            LocalTime endTime,
            Double confidence
    ) {
    }
}
