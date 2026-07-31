package com.nhnacademy.recommendation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.llm.AnswerDto;
import com.nhnacademy.recommendation.dto.llm.LlmConversationContext;
import com.nhnacademy.recommendation.dto.llm.LlmRequestDto;
import com.nhnacademy.recommendation.dto.llm.LlmResponseDto;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityDto;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityType;
import com.nhnacademy.recommendation.exception.InvalidMessageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class LlmServiceTest {

    @Mock
    ChatClient chatClient;

    @Mock
    ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    LlmConversationContextService conversationContextService;

    LlmRequestContextHolder contextHolder;
    LlmService llmService;

    @BeforeEach
    void setUp() {
        contextHolder = new LlmRequestContextHolder();
        llmService = new LlmService(chatClient, contextHolder, conversationContextService, new ObjectMapper());
    }

    @Test
    @DisplayName("LLM JSON 응답을 AnswerDto로 파싱하고 최근 질문/답변을 Redis context에 저장한다")
    void answer() {
        LlmConversationContext context = new LlmConversationContext(
                "이전 질문",
                "이전 답변",
                List.of(new MentionedEntityDto(MentionedEntityType.TEAM, 3L, null)),
                LocalDateTime.of(2026, 7, 31, 9, 0)
        );
        LlmRequestDto request = new LlmRequestDto(null, null, "건물 목록 보여줘", LocalDateTime.of(2026, 7, 31, 10, 0));

        given(conversationContextService.find(1L)).willReturn(context, context);
        given(chatClient.prompt()).willReturn(requestSpec);
        given(requestSpec.user(anyString())).willReturn(requestSpec);
        given(requestSpec.call()).willReturn(callResponseSpec);
        given(callResponseSpec.content()).willReturn("""
                {
                  "answer": "3번팀 건물 목록입니다.",
                  "options": []
                }
                """);

        LlmResponseDto response = llmService.answer(1L, UserRole.NORMAL, request);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getMessage()).isEqualTo("건물 목록 보여줘");
        assertThat(response.getAnswer()).isEqualTo(new AnswerDto("3번팀 건물 목록입니다.", List.of()));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("최근 언급 엔티티:")
                .contains("TEAM: id=3")
                .contains("현재 질문:")
                .contains("건물 목록 보여줘");

        ArgumentCaptor<LlmConversationContext> savedContextCaptor = ArgumentCaptor.forClass(LlmConversationContext.class);
        verify(conversationContextService, times(2)).save(org.mockito.ArgumentMatchers.eq(1L), savedContextCaptor.capture());
        assertThat(savedContextCaptor.getAllValues().getLast().lastQuestion()).isEqualTo("건물 목록 보여줘");
        assertThat(savedContextCaptor.getAllValues().getLast().lastAnswer()).isEqualTo("3번팀 건물 목록입니다.");
    }

    @Test
    @DisplayName("메시지가 비어 있으면 LLM을 호출하지 않고 InvalidMessageException을 던진다")
    void blankMessage() {
        LlmRequestDto request = new LlmRequestDto(null, null, " ", LocalDateTime.of(2026, 7, 31, 10, 0));

        assertThatThrownBy(() -> llmService.answer(1L, UserRole.NORMAL, request))
                .isInstanceOf(InvalidMessageException.class);

        verifyNoInteractions(chatClient);
    }
}
