package com.nhnacademy.recommendation.service;

import com.nhnacademy.recommendation.dto.llm.LlmAnswerDto;
import com.nhnacademy.recommendation.dto.llm.LlmGeneratedAnswerDto;
import com.nhnacademy.recommendation.exception.InvalidMessageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {
    private final ChatClient chatClient;

    public LlmAnswerDto answer(String message) {
        LocalDateTime requestedAt = LocalDateTime.now();
        if (message == null || message.isBlank()) {
            throw new InvalidMessageException();
        }
        log.info("요청한 메시지: {}", message);
        String systemPrompt = """
                당신은 강의실 환경관리 도우미입니다.
                
                사용자의 요청에 따라 적절한 검색 도구를 호출하여 답변하세요.
                
                따로 지역을 언급하지 않으면 광주 동구 서석동을 기준으로 답합니다.
                
                해결책을 제시할 때는 이유와 근거 및 수치를 같이 들어 제시하세요.
                
                단순한 정보 조회에 관한 답변은 status는 null로 reasons와 recommendations에는 빈 리스트로 답변하세요.
                """;

        LlmGeneratedAnswerDto chatResponse = null;
        try {
            chatResponse = chatClient.prompt()
                    .system(systemPrompt)
                    .user(message)
                    .call()
                    .entity(LlmGeneratedAnswerDto.class);
        } catch (Exception e) {
            log.warn("LLM 응답 수신 실패 (Tool 호출은 성공할 수 있음): {}", e.getMessage());
        }
        String answer = parseAnswer(Objects.requireNonNull(chatResponse));

        return new LlmAnswerDto(message, answer, requestedAt);
    }

    public String parseAnswer(LlmGeneratedAnswerDto response) {
        StringBuilder builder = new StringBuilder();
        if (response.status() != null) {
            builder.append(response.status().toString());
        }
        builder.append("\n\n")
                .append(response.answer());
        if (!response.reasons().isEmpty()) {
            builder.append("\n\n왜 그런가요?\n\n");
            int number = 1;
            for (String reason : response.reasons()) {
                builder.append(number++)
                        .append(". ")
                        .append(reason)
                        .append("\n");
            }
        }
        if (!response.recommendations().isEmpty()) {
            builder.append("\n\n어떻게 해야 하나요?\n\n");
            int number = 1;
            for (String recommendation : response.recommendations()) {
                builder.append(number++)
                        .append(". ")
                        .append(recommendation)
                        .append("\n");
            }
        }
        return builder.toString();
    }
}
