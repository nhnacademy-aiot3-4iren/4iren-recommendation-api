package com.nhnacademy.recommendation.dto.llm;

import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.roomsub.RoomSubResponse;

import java.util.List;

public record LlmRequestContext(
        Long userId,
        UserRole role,
        LlmConversationContext conversationContext,
        RequestSource source,
        List<RoomSubResponse> roomSubInfo
) {
    public LlmRequestContext(Long userId, UserRole role, LlmConversationContext conversationContext) {
        this(userId, role, conversationContext, RequestSource.WEB, List.of());
    }

    public LlmRequestContext {
        if (source == null) {
            source = RequestSource.WEB;
        }
        if (roomSubInfo == null) {
            roomSubInfo = List.of();
        }
    }
}
