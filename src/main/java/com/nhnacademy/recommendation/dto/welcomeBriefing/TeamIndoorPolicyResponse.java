package com.nhnacademy.recommendation.dto.welcomeBriefing;

public record TeamIndoorPolicyResponse(
        Long teamId,
        IndoorEnvironmentPolicy defaultPolicy,
        TeamIndoorPolicyOverride overridePolicy,
        IndoorEnvironmentPolicy effectivePolicy
) {
}
