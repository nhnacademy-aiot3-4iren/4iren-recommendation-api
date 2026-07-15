package com.nhnacademy.recommendation.dto.llm;

import java.util.List;

public record LlmGeneratedAnswerDto(
        String answer,
        AnswerStatus status,
        List<String> reasons,
        List<String> recommendations
) {
}
