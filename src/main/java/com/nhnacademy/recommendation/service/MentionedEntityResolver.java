package com.nhnacademy.recommendation.service;

import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityType;
import com.nhnacademy.recommendation.dto.llm.RequestSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MentionedEntityResolver {

    private final LlmRequestContextHolder llmRequestContextHolder;
    private final LlmConversationContextService llmConversationContextService;

    public Long resolve(Long explicitId, MentionedEntityType type) {
        if (explicitId != null) {
            return explicitId;
        }

        LlmRequestContext context = llmRequestContextHolder.get();
        if (context.source() == RequestSource.TELEGRAM) {
            return context.conversationContext()
                    .findRecentEntityId(type)
                    .orElse(null);
        }

        return context.conversationContext()
                .findRecentEntityId(type)
                .or(() -> llmConversationContextService.find(context.userId()).findRecentEntityId(type))
                .orElse(null);
    }
}
