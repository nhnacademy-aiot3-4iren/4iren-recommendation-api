package com.nhnacademy.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {
    private final ChatClient chatClient;

    public String answer(String message) {
        String systemPrompt = """
                당신은 강의실 환경관리 도우미입니다.
                
                사용자의 요청에 따라 적절한 검색 도구를 호출하여 답변하세요.
                
                따로 지역을 언급하지 않으면 광주 동구 서석동을 기준으로 답합니다.
                
                해결책을 제시할 때는 이유와 근거 및 수치를 같이 들어 제시하세요.
                """;

        ChatResponse chatResponse = null;
        try {
            chatResponse = chatClient.prompt()
                    .system(systemPrompt)
                    .user(message)
                    .call().chatResponse();
        }catch (Exception e){
            log.warn("LLM 응답 수신 실패 (Tool 호출은 성공할 수 있음): {}", e.getMessage());
        }

        return chatResponse.getResult()
                .getOutput()
                .getText();
    }
}
