package com.nhnacademy.recommendation.controller;

import com.nhnacademy.recommendation.service.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class LlmController {

    private final LlmService llmService;

    @GetMapping("/llm")
    public String getAnswer(@RequestParam String message){
        return llmService.answer(message);
    }
}
