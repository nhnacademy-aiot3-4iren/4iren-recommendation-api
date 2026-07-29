package com.nhnacademy.recommendation.dto.llm;

import java.util.List;

public record AnswerDto(
        String answer,
        List<String> options
) {
}
