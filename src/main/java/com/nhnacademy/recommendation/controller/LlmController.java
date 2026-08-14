package com.nhnacademy.recommendation.controller;

import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.llm.LlmRequestDto;
import com.nhnacademy.recommendation.dto.llm.LlmResponseDto;
import com.nhnacademy.recommendation.service.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class LlmController {

    private final LlmService llmService;

    @PostMapping("/chat")
    public LlmResponseDto getChatAnswer(@RequestHeader(value = "X-USER-ID") Long userId,
                                      @RequestHeader(value = "X-USER-ROLE") UserRole role,
                                      @RequestHeader(value = "X-CLIENT-TYPE", required = false) String clientType,
                                      @RequestBody LlmRequestDto request) {
        return llmService.answer(userId, role, clientType, request);
    }
}
