package com.nhnacademy.recommendation.model.behavior;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record BehaviorRecommendation(
        String schemaVersion,
        Context context,
        String recommendationType,
        List<ScheduleItem> recommendedSchedule
) {

    public BehaviorRecommendation {
        recommendedSchedule = List.copyOf(recommendedSchedule);
    }

    public record Context(
            LocalDate predictionDate,
            DayOfWeek weekday,
            Long roomId,
            String location,
            String timezone
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ScheduleItem(
            String deviceType,
            String action,
            @JsonFormat(pattern = "HH:mm") LocalTime startTime,
            @JsonFormat(pattern = "HH:mm") LocalTime endTime,
            double confidence
    ) {
    }
}
