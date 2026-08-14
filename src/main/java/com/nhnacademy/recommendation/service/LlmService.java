package com.nhnacademy.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.llm.*;
import com.nhnacademy.recommendation.exception.InvalidMessageException;
import com.nhnacademy.recommendation.util.TimingLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class LlmService {
    private final ChatClient chatClient;
    private final LlmRequestContextHolder llmRequestContextHolder;
    private final LlmConversationContextService llmConversationContextService;
    private final ObjectMapper objectMapper;

    public LlmService(@Qualifier("routingChatClient") ChatClient chatClient,
                      LlmRequestContextHolder llmRequestContextHolder,
                      LlmConversationContextService llmConversationContextService,
                      ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.llmRequestContextHolder = llmRequestContextHolder;
        this.llmConversationContextService = llmConversationContextService;
        this.objectMapper = objectMapper;
    }

    public LlmResponseDto answer(Long headerUserId, UserRole role, String clientType, LlmRequestDto request) {
        try (TimingLog.Timer ignored = TimingLog.start(log, "[Timing][Request] total")) {
            LocalDateTime receivedAt = LocalDateTime.now();
            if (request == null || request.message() == null || request.message().isBlank()) {
                throw new InvalidMessageException();
            }
            Long userId = headerUserId;
            UserRole resolvedRole = role == null ? UserRole.NORMAL : role;
            RequestSource source = RequestSource.from(clientType);
            LlmConversationContext conversationContext = resolveConversationContext(userId, source);

            log.info("User ID: {}, requestSource: {}", userId, source);
            try {
                llmRequestContextHolder.set(new LlmRequestContext(userId, resolvedRole, conversationContext, source, request.roomSubInfo()));
                String userPrompt = """
                        최근 언급 엔티티:
                        %s
                        
                        구독 중인 강의실:
                        %s
                        
                        최근 대화:
                        %s
                        
                        현재 질문:
                        %s
                        """.formatted(
                        formatRecentMentions(conversationContext),
                        formatRoomSubInfo(request.roomSubInfo()),
                        formatRecentConversation(conversationContext),
                        request.message()
                );

                log.info("요청한 메시지: {}", userPrompt);
                String rawAnswer = TimingLog.measure(log, "[Timing][LLM] routingChatClient", () -> chatClient.prompt()
                        .user(userPrompt)
                        .call()
                        .content());

                AnswerDto answer = TimingLog.measure(log, "[Timing][LLM] AnswerDto parse", () -> parseAnswer(rawAnswer));

                if (source == RequestSource.WEB) {
                    TimingLog.run(log, "[Timing][Redis] save last exchange", () -> {
                        LlmConversationContext latestContext = llmConversationContextService.find(userId);
                        llmConversationContextService.save(userId, latestContext.withLastExchange(request.message(), answer.answer()));
                    });
                }

                LlmResponseDto answerDto = new LlmResponseDto(request.message(), answer, request.requestedAt(), receivedAt);
                answerDto.setUserId(userId);
                conversationContext.findRecentEntityId(MentionedEntityType.ROOM).ifPresent(answerDto::setRoomId);
                return answerDto;
            } finally {
                llmRequestContextHolder.clear();
            }
        }


    }

    private AnswerDto parseAnswer(String rawAnswer) {
        if (rawAnswer == null || rawAnswer.isBlank()) {
            return new AnswerDto("답변을 생성하지 못했습니다.", List.of());
        }

        String json = stripMarkdownCodeFence(rawAnswer.trim());
        try {
            AnswerDto answer = objectMapper.readValue(json, AnswerDto.class);
            return new AnswerDto(
                    firstNonBlank(answer.answer(), rawAnswer),
                    answer.options() == null ? List.of() : answer.options()
            );
        } catch (JsonProcessingException e) {
            log.warn("AnswerDto 파싱 실패. 문자열 답변으로 처리합니다. rawAnswer={}", rawAnswer, e);
            return new AnswerDto(rawAnswer, List.of());
        }
    }

    private String stripMarkdownCodeFence(String content) {
        if (!content.startsWith("```")) {
            return content;
        }

        String stripped = content.replaceFirst("^```(?:json)?\\s*", "");
        return stripped.replaceFirst("\\s*```$", "").trim();
    }

    private LlmConversationContext resolveConversationContext(Long userId, RequestSource source) {
        if (source == RequestSource.TELEGRAM) {
            Long roomId = llmConversationContextService.findTelegramLastMentionedRoomId(userId);
            if (roomId == null) {
                return LlmConversationContext.empty();
            }
            return LlmConversationContext.empty()
                    .withMention(new MentionedEntityDto(MentionedEntityType.ROOM, roomId, null));
        }
        return llmConversationContextService.find(userId);
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

    private String formatRoomSubInfo(List<com.nhnacademy.recommendation.dto.roomsub.RoomSubResponse> roomSubInfo) {
        if (roomSubInfo == null || roomSubInfo.isEmpty()) {
            return "없음";
        }

        StringBuilder builder = new StringBuilder();
        for (com.nhnacademy.recommendation.dto.roomsub.RoomSubResponse room : roomSubInfo) {
            builder.append("- roomId=")
                    .append(room.roomId())
                    .append(", roomName=")
                    .append(firstNonBlank(room.roomName(), ""))
                    .append(", notificationEnabled=")
                    .append(room.notificationEnabled())
                    .append("\n");
        }
        return builder.toString();
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
