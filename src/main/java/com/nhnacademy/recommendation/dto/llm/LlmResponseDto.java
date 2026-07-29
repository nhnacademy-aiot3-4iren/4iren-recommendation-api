package com.nhnacademy.recommendation.dto.llm;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LlmResponseDto {
    private Long userId;
    private Long roomId;
    private final String message;
    private final AnswerDto answer;
    private final LocalDateTime requestedAt;
    private final LocalDateTime receivedAt;
    private final LocalDateTime answeredAt;

    public LlmResponseDto(String message, AnswerDto answer, LocalDateTime requestedAt, LocalDateTime receivedAt) {
        userId = null;
        roomId = null;
        this.message = message;
        this.answer = answer;
        this.requestedAt = requestedAt;
        this.receivedAt = receivedAt;
        answeredAt = LocalDateTime.now();
    }

}
