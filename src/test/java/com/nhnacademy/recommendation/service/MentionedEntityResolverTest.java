package com.nhnacademy.recommendation.service;

import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.llm.LlmConversationContext;
import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityDto;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MentionedEntityResolverTest {

    @Mock
    LlmConversationContextService conversationContextService;

    LlmRequestContextHolder contextHolder = new LlmRequestContextHolder();

    @AfterEach
    void tearDown() {
        contextHolder.clear();
    }

    @Test
    @DisplayName("명시적 ID가 있으면 request context와 Redis를 조회하지 않고 그대로 반환한다")
    void resolveExplicitId() {
        MentionedEntityResolver resolver = new MentionedEntityResolver(contextHolder, conversationContextService);

        Long result = resolver.resolve(3L, MentionedEntityType.TEAM);

        assertThat(result).isEqualTo(3L);
        verifyNoInteractions(conversationContextService);
    }

    @Test
    @DisplayName("명시적 ID가 없으면 현재 request context의 최근 언급 ID를 반환한다")
    void resolveFromRequestContext() {
        MentionedEntityResolver resolver = new MentionedEntityResolver(contextHolder, conversationContextService);
        LlmConversationContext context = new LlmConversationContext(
                null,
                null,
                List.of(new MentionedEntityDto(MentionedEntityType.BUILDING, 10L, "본관")),
                null
        );
        contextHolder.set(new LlmRequestContext(1L, UserRole.NORMAL, context));

        Long result = resolver.resolve(null, MentionedEntityType.BUILDING);

        assertThat(result).isEqualTo(10L);
        verifyNoInteractions(conversationContextService);
    }

    @Test
    @DisplayName("request context에 없으면 Redis의 최근 언급 ID를 반환한다")
    void resolveFromRedisContext() {
        MentionedEntityResolver resolver = new MentionedEntityResolver(contextHolder, conversationContextService);
        contextHolder.set(new LlmRequestContext(1L, UserRole.NORMAL, LlmConversationContext.empty()));
        LlmConversationContext redisContext = new LlmConversationContext(
                null,
                null,
                List.of(new MentionedEntityDto(MentionedEntityType.ROOM, 20L, "201호")),
                null
        );
        given(conversationContextService.find(1L)).willReturn(redisContext);

        Long result = resolver.resolve(null, MentionedEntityType.ROOM);

        assertThat(result).isEqualTo(20L);
    }

    @Test
    @DisplayName("request context와 Redis에 모두 없으면 null을 반환한다")
    void resolveMissingEntity() {
        MentionedEntityResolver resolver = new MentionedEntityResolver(contextHolder, conversationContextService);
        contextHolder.set(new LlmRequestContext(1L, UserRole.NORMAL, LlmConversationContext.empty()));
        given(conversationContextService.find(1L)).willReturn(LlmConversationContext.empty());

        Long result = resolver.resolve(null, MentionedEntityType.ROOM);

        assertThat(result).isNull();
    }
}
