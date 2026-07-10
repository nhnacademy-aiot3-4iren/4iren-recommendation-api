package com.nhnacademy.recommendation.config;

import com.nhnacademy.recommendation.tools.CurrentWeatherTool;
import com.nhnacademy.recommendation.tools.ForecastWeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient geminiChatClient(@Qualifier(value = "googleGenAiChatModel") ChatModel geminiModel,
                                       CurrentWeatherTool currentWeatherTool,
                                       ForecastWeatherTool forecastWeatherTool){
        return ChatClient.builder(geminiModel)
                .defaultSystem("당신은 도구를 자유자재로 활용하여 날씨 정보와 강의실 환경 정보를 얻어 답을 제시해주는 유능한 어시스턴트입니다.")
                .defaultTools(currentWeatherTool, forecastWeatherTool)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
