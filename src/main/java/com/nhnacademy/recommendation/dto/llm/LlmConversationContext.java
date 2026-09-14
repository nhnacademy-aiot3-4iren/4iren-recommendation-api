package com.nhnacademy.recommendation.dto.llm;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record LlmConversationContext(
        String lastQuestion,
        String lastAnswer,
        List<MentionedEntityDto> mentions,
        LocalDateTime updatedAt
) {

    public static LlmConversationContext empty() {
        return new LlmConversationContext(null, null, List.of(), null);
    }

    public Optional<Long> findRecentEntityId(MentionedEntityType type) {
        if (mentions == null) {
            return Optional.empty();
        }
        return mentions.stream()
                .filter(mention -> mention.type() == type)
                .map(MentionedEntityDto::id)
                .findFirst();
    }

    public LlmConversationContext withLastExchange(String question, String answer) {
        return new LlmConversationContext(question, answer, mentionsOrEmpty(), LocalDateTime.now());
    }

    public LlmConversationContext withMention(MentionedEntityDto mention) {
        MentionedEntityDto normalizedMention = preserveExistingName(mention);
        List<MentionedEntityDto> updatedMentions = new ArrayList<>();
        updatedMentions.add(normalizedMention);
        mentionsOrEmpty().stream()
                .filter(existing -> existing.type() != normalizedMention.type())
                .limit(9)
                .forEach(updatedMentions::add);
        return new LlmConversationContext(lastQuestion, lastAnswer, List.copyOf(updatedMentions), LocalDateTime.now());
    }

    private MentionedEntityDto preserveExistingName(MentionedEntityDto mention) {
        if (mention.name() != null && !mention.name().isBlank()) {
            return mention;
        }
        return mentionsOrEmpty().stream()
                .filter(existing -> existing.type() == mention.type())
                .filter(existing -> Objects.equals(existing.id(), mention.id()))
                .filter(existing -> existing.name() != null && !existing.name().isBlank())
                .findFirst()
                .map(existing -> new MentionedEntityDto(mention.type(), mention.id(), existing.name()))
                .orElse(mention);
    }

    private List<MentionedEntityDto> mentionsOrEmpty() {
        return mentions == null ? List.of() : mentions;
    }
}
