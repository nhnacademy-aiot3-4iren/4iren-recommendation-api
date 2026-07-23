package com.nhnacademy.recommendation.dto.llm;

import java.time.LocalDateTime;
import java.util.List;

public record LlmRequestDto(
        String userId,
        Long lastMentionRoomId,
        List<Long> subscribedRoomIds,
        String message,
        LocalDateTime requestedAt
) {
}
