package com.nhnacademy.recommendation.service;

import com.nhnacademy.recommendation.dto.llm.LlmAnswerDto;
import com.nhnacademy.recommendation.dto.llm.LlmRequestDto;
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

    public LlmService(@Qualifier("routingChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public LlmAnswerDto answer(String headerUserId, LlmRequestDto request) {
        String userId = resolveUserId(headerUserId, request.userId());
        LocalDateTime receivedAt = LocalDateTime.now();
        if (request == null || request.message().isBlank()) {
            throw new InvalidMessageException();
        }
        if (userId != null) {
            log.info("User ID: {}", request.userId());
        }

        log.info("요청한 메시지: {}", request.message());
        String answer = chatClient.prompt()
                .user(request.message())
                .call()
                .content();

        LlmAnswerDto answerDto = new LlmAnswerDto(request.message(), answer, request.requestedAt(), receivedAt);
        answerDto.setUserId(userId);
        return answerDto;
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
