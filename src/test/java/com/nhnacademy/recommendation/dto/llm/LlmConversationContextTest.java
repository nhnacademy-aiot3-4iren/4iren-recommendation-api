package com.nhnacademy.recommendation.dto.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LlmConversationContextTest {

    @Test
    @DisplayName("empty는 최근 질문, 답변, 언급 엔티티가 없는 컨텍스트를 반환한다")
    void empty() {
        LlmConversationContext context = LlmConversationContext.empty();

        assertThat(context.lastQuestion()).isNull();
        assertThat(context.lastAnswer()).isNull();
        assertThat(context.mentions()).isEmpty();
        assertThat(context.updatedAt()).isNull();
    }

    @Test
    @DisplayName("withLastExchange는 기존 언급 엔티티를 유지하고 최근 질문과 답변을 갱신한다")
    void withLastExchange() {
        MentionedEntityDto team = new MentionedEntityDto(MentionedEntityType.TEAM, 3L, null);
        LlmConversationContext context = new LlmConversationContext(null, null, List.of(team), null);

        LlmConversationContext updated = context.withLastExchange("질문", "답변");

        assertThat(updated.lastQuestion()).isEqualTo("질문");
        assertThat(updated.lastAnswer()).isEqualTo("답변");
        assertThat(updated.mentions()).containsExactly(team);
        assertThat(updated.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("withMention은 새 언급 엔티티를 가장 앞으로 저장한다")
    void withMentionAddsNewMentionFirst() {
        MentionedEntityDto team = new MentionedEntityDto(MentionedEntityType.TEAM, 3L, null);
        MentionedEntityDto building = new MentionedEntityDto(MentionedEntityType.BUILDING, 10L, "본관");
        LlmConversationContext context = new LlmConversationContext("질문", "답변", List.of(team), LocalDateTime.now());

        LlmConversationContext updated = context.withMention(building);

        assertThat(updated.mentions()).containsExactly(building, team);
        assertThat(updated.lastQuestion()).isEqualTo("질문");
        assertThat(updated.lastAnswer()).isEqualTo("답변");
    }

    @Test
    @DisplayName("withMention은 같은 타입의 기존 언급 엔티티를 최신 값으로 교체한다")
    void withMentionReplacesSameType() {
        MentionedEntityDto oldRoom = new MentionedEntityDto(MentionedEntityType.ROOM, 1L, "101호");
        MentionedEntityDto team = new MentionedEntityDto(MentionedEntityType.TEAM, 3L, null);
        MentionedEntityDto newRoom = new MentionedEntityDto(MentionedEntityType.ROOM, 2L, "102호");
        LlmConversationContext context = new LlmConversationContext(null, null, List.of(oldRoom, team), null);

        LlmConversationContext updated = context.withMention(newRoom);

        assertThat(updated.mentions()).containsExactly(newRoom, team);
        assertThat(updated.findRecentEntityId(MentionedEntityType.ROOM)).contains(2L);
    }

    @Test
    @DisplayName("withMention은 최대 10개의 최신 언급 엔티티만 유지한다")
    void withMentionKeepsUpToTenMentions() {
        LlmConversationContext context = new LlmConversationContext(
                null,
                null,
                List.of(
                        new MentionedEntityDto(MentionedEntityType.TEAM, 1L, null),
                        new MentionedEntityDto(MentionedEntityType.BUILDING, 2L, null),
                        new MentionedEntityDto(MentionedEntityType.ROOM, 3L, null),
                        new MentionedEntityDto(MentionedEntityType.SENSOR, 4L, null),
                        new MentionedEntityDto(MentionedEntityType.DEVICE, 5L, null),
                        new MentionedEntityDto(MentionedEntityType.TEAM, 6L, null),
                        new MentionedEntityDto(MentionedEntityType.BUILDING, 7L, null),
                        new MentionedEntityDto(MentionedEntityType.ROOM, 8L, null),
                        new MentionedEntityDto(MentionedEntityType.SENSOR, 9L, null),
                        new MentionedEntityDto(MentionedEntityType.DEVICE, 10L, null),
                        new MentionedEntityDto(MentionedEntityType.TEAM, 11L, null)
                ),
                null
        );

        LlmConversationContext updated = context.withMention(new MentionedEntityDto(MentionedEntityType.ROOM, 12L, null));

        assertThat(updated.mentions()).hasSize(10);
        assertThat(updated.mentions().getFirst()).isEqualTo(new MentionedEntityDto(MentionedEntityType.ROOM, 12L, null));
        assertThat(updated.mentions()).noneMatch(mention -> mention.type() == MentionedEntityType.ROOM && mention.id().equals(3L));
    }

    @Test
    @DisplayName("findRecentEntityId는 해당 타입의 가장 최근 ID를 반환한다")
    void findRecentEntityId() {
        LlmConversationContext context = new LlmConversationContext(
                null,
                null,
                List.of(
                        new MentionedEntityDto(MentionedEntityType.BUILDING, 10L, "본관"),
                        new MentionedEntityDto(MentionedEntityType.TEAM, 3L, null)
                ),
                null
        );

        assertThat(context.findRecentEntityId(MentionedEntityType.BUILDING)).contains(10L);
        assertThat(context.findRecentEntityId(MentionedEntityType.ROOM)).isEmpty();
    }
}
