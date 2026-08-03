package com.nhnacademy.recommendation.dto.llm;

import com.nhnacademy.recommendation.dto.UserRole;

public record LlmRequestContext(
        Long userId,
        UserRole role,
        LlmConversationContext conversationContext
) {
}
