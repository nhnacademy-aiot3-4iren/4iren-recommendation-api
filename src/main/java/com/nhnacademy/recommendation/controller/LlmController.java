package com.nhnacademy.recommendation.controller;

import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.llm.LlmResponseDto;
import com.nhnacademy.recommendation.dto.llm.LlmRequestDto;
import com.nhnacademy.recommendation.service.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
public class LlmController {

    private final LlmService llmService;

    @PostMapping("/chat")
    public LlmResponseDto getChatAnswer(@RequestHeader(value = "X-USER-ID", required = false) Long userId,
                                      @RequestHeader(value = "X-USER-ROLE", required = false) UserRole role,
                                      @RequestBody LlmRequestDto request) {
        return llmService.answer(userId, role, request);
    }
}
