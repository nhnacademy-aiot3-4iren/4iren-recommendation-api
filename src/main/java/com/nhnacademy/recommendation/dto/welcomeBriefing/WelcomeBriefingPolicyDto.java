package com.nhnacademy.recommendation.dto.welcomeBriefing;

public record WelcomeBriefingPolicyDto(
        int rainPossibleProbability,
        int rainExpectedProbability,
        double strongWindSpeed,
        int highHumidityPercent,
        boolean enabled
) {

    public static WelcomeBriefingPolicyDto defaultPolicy() {
        return new WelcomeBriefingPolicyDto(30, 60, 8.0, 70, true);
    }
}
