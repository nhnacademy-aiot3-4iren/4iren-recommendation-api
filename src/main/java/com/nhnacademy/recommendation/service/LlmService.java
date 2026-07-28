package com.nhnacademy.recommendation.service;

import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.llm.LlmConversationContext;
import com.nhnacademy.recommendation.dto.llm.LlmAnswerDto;
import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import com.nhnacademy.recommendation.dto.llm.LlmRequestDto;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityDto;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityType;
import com.nhnacademy.recommendation.exception.InvalidMessageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class LlmService {
    private final ChatClient chatClient;
    private final LlmRequestContextHolder llmRequestContextHolder;
    private final LlmConversationContextService llmConversationContextService;

    public LlmService(@Qualifier("routingChatClient") ChatClient chatClient,
                      LlmRequestContextHolder llmRequestContextHolder,
                      LlmConversationContextService llmConversationContextService) {
        this.chatClient = chatClient;
        this.llmRequestContextHolder = llmRequestContextHolder;
        this.llmConversationContextService = llmConversationContextService;
    }

    public LlmAnswerDto answer(String headerUserId, UserRole role, LlmRequestDto request) {
        LocalDateTime receivedAt = LocalDateTime.now();
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new InvalidMessageException();
        }
        String userId = resolveUserId(headerUserId, request.userId());
        UserRole resolvedRole = role == null ? UserRole.NORMAL : role;
        LlmConversationContext conversationContext = resolveConversationContext(userId, request);

        log.info("User ID: {}", userId);
        try {
            llmConversationContextService.save(userId, conversationContext);
            llmRequestContextHolder.set(new LlmRequestContext(userId, resolvedRole, conversationContext));
            String userPrompt = """
                    최근 언급 엔티티:
                    %s
                    
                    최근 대화:
                    %s
                    
                    현재 질문:
                    %s
                    """.formatted(
                    formatRecentMentions(conversationContext),
                    formatRecentConversation(conversationContext),
                    request.message()
            );

            log.info("요청한 메시지: {}", userPrompt);
            String answer = chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .content();

            LlmConversationContext latestContext = llmConversationContextService.find(userId);
            llmConversationContextService.save(userId, latestContext.withLastExchange(request.message(), answer));

            LlmAnswerDto answerDto = new LlmAnswerDto(request.message(), answer, request.requestedAt(), receivedAt);
            answerDto.setUserId(userId);
            return answerDto;
        } finally {
            llmRequestContextHolder.clear();
        }


    }

    private LlmConversationContext resolveConversationContext(String userId, LlmRequestDto request) {
        LlmConversationContext context = llmConversationContextService.find(userId);
        if (request.lastQuestion() != null || request.lastAnswer() != null) {
            String lastQuestion = firstNonBlank(context.lastQuestion(), request.lastQuestion());
            String lastAnswer = firstNonBlank(context.lastAnswer(), request.lastAnswer());
            context = context.withLastExchange(lastQuestion, lastAnswer);
        }
        if (request.lastMentionRoomId() != null) {
            context = context.withMention(new MentionedEntityDto(MentionedEntityType.ROOM, request.lastMentionRoomId(), null));
        }
        return context;
    }

    private String formatRecentConversation(LlmConversationContext context) {
        String lastAnswer = firstNonBlank(context.lastAnswer(), "없음");
        String lastQuestion = firstNonBlank(context.lastQuestion(), "없음");

        return """
                이전 질문: %s
                이전 답변: %s
                """.formatted(
                lastQuestion,
                lastAnswer
        );
    }

    private String formatRecentMentions(LlmConversationContext context) {
        if (context.mentions() == null || context.mentions().isEmpty()) {
            return "없음";
        }

        StringBuilder builder = new StringBuilder();
        for (MentionedEntityDto mention : context.mentions()) {
            builder.append("- ")
                    .append(mention.type())
                    .append(": id=")
                    .append(mention.id());
            if (mention.name() != null && !mention.name().isBlank()) {
                builder.append(", name=").append(mention.name());
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private String resolveUserId(String headerUserId, String bodyUserId) {
        boolean hasHeaderUserId = headerUserId != null && !headerUserId.isBlank();
        boolean hasBodyUserId = bodyUserId != null && !bodyUserId.isBlank();

        if (hasHeaderUserId && hasBodyUserId && !headerUserId.equals(bodyUserId)) {
            throw new IllegalArgumentException("헤더와 본문의 사용자 ID가 일치하지 않습니다.");
        }

        if (hasHeaderUserId) {
            return headerUserId;
        }

        if (hasBodyUserId) {
            return bodyUserId;
        }

        throw new IllegalArgumentException("사용자 ID가 필요합니다.");
    }
}
