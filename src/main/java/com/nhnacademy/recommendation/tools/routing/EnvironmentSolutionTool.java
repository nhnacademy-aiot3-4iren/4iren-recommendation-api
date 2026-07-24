package com.nhnacademy.recommendation.tools.routing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EnvironmentSolutionTool {

    private final ChatClient chatClient;

    public EnvironmentSolutionTool(@Qualifier("environmentSolutionChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Tool(
            name = "environment_solution",
            description = """
                    실내 환경, CO2, 온도, 습도, 환기, 문/창문 개방,
                    외부 날씨를 고려한 환경관리 조언이 필요할 때 호출합니다.
                    """
    )
    public String answerEnvironmentQuestion(
            @ToolParam(description = "사용자의 원본 질문") String userQuestion) {
        log.info("[Environment Solution Tool] 환경관리 답변 생성 호출");

        return chatClient.prompt()
                .user(userQuestion)
                .call()
                .content();
    }
}
