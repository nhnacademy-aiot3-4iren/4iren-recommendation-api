package com.nhnacademy.recommendation.dto.llm;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LlmAnswerDto {
    private String userId;
    private Long roomId;
    private final String message;
    private final String answer;
    private final LocalDateTime requestedAt;
    private final LocalDateTime receivedAt;
    private final LocalDateTime answeredAt;

    public LlmAnswerDto(String message, String answer, LocalDateTime requestedAt, LocalDateTime receivedAt) {
        userId = null;
        roomId = null;
        this.message = message;
        this.answer = answer;
        this.requestedAt = requestedAt;
        this.receivedAt = receivedAt;
        answeredAt = LocalDateTime.now();
    }

}
