package com.nhnacademy.recommendation.dto.llm;

public record TelegramConversationContext(
        String intentType,
        String lastQuestion,
        String lastAnswer
) {
    public boolean isQuestion() {
        return "QUESTION".equalsIgnoreCase(intentType);
    }
}
