package com.nhnacademy.recommendation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.llm.AnswerDto;
import com.nhnacademy.recommendation.dto.llm.LlmRequestDto;
import com.nhnacademy.recommendation.dto.llm.LlmResponseDto;
import com.nhnacademy.recommendation.exception.GlobalExceptionHandler;
import com.nhnacademy.recommendation.exception.InvalidMessageException;
import com.nhnacademy.recommendation.service.LlmService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(LlmController.class)
@Import(GlobalExceptionHandler.class)
class LlmControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    LlmService llmService;

    @Test
    @DisplayName("POST /chat 요청을 서비스에 위임하고 응답 DTO를 반환한다")
    void getChatAnswer() throws Exception {
        LlmRequestDto request = new LlmRequestDto(null, "3번팀 건물 목록", LocalDateTime.of(2026, 7, 31, 10, 0));
        LlmResponseDto response = new LlmResponseDto(
                request.message(),
                new AnswerDto("3번팀 건물 목록입니다.", List.of()),
                request.requestedAt(),
                LocalDateTime.of(2026, 7, 31, 10, 0, 1)
        );
        response.setUserId(1L);

        given(llmService.answer(eq(1L), eq(UserRole.NORMAL), isNull(), any(LlmRequestDto.class))).willReturn(response);

        mockMvc.perform(post("/api/recommendation/chat")
                        .header("X-USER-ID", "1")
                        .header("X-USER-ROLE", "NORMAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.message").value("3번팀 건물 목록"))
                .andExpect(jsonPath("$.answer.answer").value("3번팀 건물 목록입니다."))
                .andExpect(jsonPath("$.answer.options").isArray());
    }

    @Test
    @DisplayName("서비스에서 InvalidMessageException이 발생하면 400 응답을 반환한다")
    void invalidMessage() throws Exception {
        LlmRequestDto request = new LlmRequestDto(null, "", LocalDateTime.of(2026, 7, 31, 10, 0));
        given(llmService.answer(eq(1L), eq(UserRole.NORMAL), isNull(), any(LlmRequestDto.class))).willThrow(new InvalidMessageException());

        mockMvc.perform(post("/api/recommendation/chat")
                        .header("X-USER-ID", "1")
                        .header("X-USER-ROLE", "NORMAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("메시지는 null이거나 빈 값일 수 없습니다."));
    }
}
