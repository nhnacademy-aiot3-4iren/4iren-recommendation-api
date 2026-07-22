package com.nhnacademy.recommendation.dto.llm;

import java.time.LocalDateTime;

public record LlmRequestDto(
        String userId,
        String message,
        LocalDateTime requestedAt
) {
}
