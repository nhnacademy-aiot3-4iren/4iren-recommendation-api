package com.nhnacademy.recommendation.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SensorDataAnswerTool {

    private final ChatClient chatClient;

    public SensorDataAnswerTool(@Qualifier("sensorDataAnswerChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Tool(
            name = "sensor_data_answer",
            description = """
                    저장된 센서 측정값, 과거 기록, 최근값, 평균, 최대/최소,
                    특정 기간의 통계나 추세 조회가 필요할 때 호출합니다.
                    """
    )
    public String answerSensorDataQuestion(
            @ToolParam(description = "사용자의 원본 질문") String userQuestion) {
        log.info("[Sensor Data Answer Tool] 센서 데이터 답변 생성 호출");

        return chatClient.prompt()
                .user(userQuestion)
                .call()
                .content();
    }
}
