package com.nhnacademy.recommendation.dto.dailysummary;

import java.util.List;

public record DailySummaryResponse(
        String summary,
        String indoorEnvironment,
        String outdoorEnvironment,
        String comparison,
        List<String> recommendations,
        List<String> checks
) {
}
