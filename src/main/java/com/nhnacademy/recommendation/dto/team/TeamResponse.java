package com.nhnacademy.recommendation.dto.team;

public record TeamResponse(
        Long teamId,
        String teamName,
        String description,
        TeamRole myRole
) {
}
