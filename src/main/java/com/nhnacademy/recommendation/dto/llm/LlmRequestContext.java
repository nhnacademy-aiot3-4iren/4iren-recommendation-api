package com.nhnacademy.recommendation.dto.llm;

import com.nhnacademy.recommendation.dto.UserRole;

public record LlmRequestContext(
        String userId,
        UserRole role,
        LlmConversationContext conversationContext
) {
}
