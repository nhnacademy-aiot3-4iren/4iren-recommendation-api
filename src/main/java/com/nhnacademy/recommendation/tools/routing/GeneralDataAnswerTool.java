package com.nhnacademy.recommendation.tools.routing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GeneralDataAnswerTool {

    private final ChatClient chatClient;

    public GeneralDataAnswerTool(@Qualifier("generalDataChatClient") ChatClient chatClient){
        this.chatClient = chatClient;
    }

    @Tool(
            name = "general_data",
            description = """
                    건물, 강의실, 강의실 내 기기, 센서,
                    팀 구조에 관련된 정보가 필요할 때 호출합니다.
                    """
    )
    public String answerGeneralDataQuestion(
            @ToolParam(description = "사용자의 원본 질문") String userQuestion) {
        log.info("[General Data Answer Tool] 구조 정보 답변 생성 호출");

        return chatClient.prompt()
                .user(userQuestion)
                .call()
                .content();
    }
}
