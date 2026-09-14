package com.nhnacademy.recommendation.dto.dailysummary;

import java.time.LocalDate;

public record DailySummaryRequest(
        Long teamId,
        Long roomId,
        LocalDate date,
        Integer startHour,
        Integer endHour
) {
}
