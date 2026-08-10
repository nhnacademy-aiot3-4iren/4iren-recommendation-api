package com.nhnacademy.recommendation.dto.welcomeBriefing;

import java.time.LocalDateTime;

public record WelcomeBriefingPolicyResponse(
        Long id,
        Long teamId,
        Long roomId,
        int rainPossibleProbability,
        int rainExpectedProbability,
        double strongWindSpeed,
        int highHumidityPercent,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
