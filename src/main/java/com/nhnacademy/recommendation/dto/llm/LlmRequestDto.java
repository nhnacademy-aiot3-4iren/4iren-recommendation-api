package com.nhnacademy.recommendation.dto.llm;

import java.time.LocalDateTime;
import java.util.List;

public record LlmRequestDto(
        String userId,
        Long lastMentionRoomId,
        List<Long> subscribedRoomIds,
        String message,
        //TODO 추후 최근 질문, 답변 필드 제거
        String lastQuestion,
        String lastAnswer,
        LocalDateTime requestedAt
) {
}
