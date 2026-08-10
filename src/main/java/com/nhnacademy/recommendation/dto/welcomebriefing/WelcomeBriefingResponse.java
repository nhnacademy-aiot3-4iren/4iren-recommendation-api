package com.nhnacademy.recommendation.dto.welcomebriefing;

import java.util.List;

public record WelcomeBriefingResponse(
        String summary,
        String currentStatus,
        String comparison,
        List<String> recommendations,
        List<String> checks
) {
}
