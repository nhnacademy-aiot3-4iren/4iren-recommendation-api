package com.nhnacademy.recommendation.dto.llm;

public record MentionedEntityDto(
        MentionedEntityType type,
        Long id,
        String name
) {
}
