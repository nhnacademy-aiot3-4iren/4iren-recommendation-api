package com.nhnacademy.recommendation.config;

import com.nhnacademy.recommendation.tools.general.CurrentWeatherTool;
import com.nhnacademy.recommendation.tools.general.SearchBuildingTool;
import com.nhnacademy.recommendation.tools.general.SearchRoomTool;
import com.nhnacademy.recommendation.tools.routing.EnvironmentSolutionTool;
import com.nhnacademy.recommendation.tools.general.ForecastWeatherTool;
import com.nhnacademy.recommendation.tools.routing.GeneralDataAnswerTool;
import com.nhnacademy.recommendation.tools.routing.SensorDataAnswerTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient routingChatClient(@Qualifier(value = "googleGenAiChatModel") ChatModel geminiModel,
                                        EnvironmentSolutionTool environmentSolutionTool,
                                        SensorDataAnswerTool sensorDataAnswerTool,
                                        GeneralDataAnswerTool generalDataAnswerTool) {
        return ChatClient.builder(geminiModel)
                .defaultSystem("""
                        당신은 사용자 질문을 적절한 전문 도구로 연결하는 라우팅 어시스턴트입니다.
                        
                        필요한 전문 도구를 호출한 뒤, 사용자에게 전달할 최종 답변 문자열만 작성하세요.
                        
                        실내 환경, CO2, 온도, 습도, 환기, 문/창문 개방,
                        외부 날씨 조언이 필요하면 environment_solution 도구를 호출하세요.
                        
                        저장된 센서 측정값, 과거 기록, 최근값, 평균, 최대/최소,
                        특정 기간의 통계나 추세 조회가 필요하면 sensor_data_answer 도구를 호출하세요.
                        
                        도구 결과에 없는 수치나 사실은 생성하지 마세요.
                        
                        어떤 전문 도구로도 답변할 수 없는 질문이면,
                        이 서비스에서 제공할 수 없는 질문이라고 답변하세요.

                        """)
                .defaultTools(environmentSolutionTool, sensorDataAnswerTool, generalDataAnswerTool)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @Bean
    public ChatClient environmentSolutionChatClient(
            @Qualifier(value = "googleGenAiChatModel") ChatModel geminiModel,
            CurrentWeatherTool currentWeatherTool,
            ForecastWeatherTool forecastWeatherTool) {
        return ChatClient.builder(geminiModel)
                .defaultSystem("""
                        당신은 강의실 환경관리 도우미입니다.
                        
                        따로 지역을 언급하지 않으면 광주 동구 서석동을 기준으로 답합니다.
                        
                        해결책을 제시할 때는 이유와 근거 및 수치를 같이 들어 제시하세요.
                        
                        외부 환경(날씨, 예보 등)에 대한 정보가 필요한 질문은 반드시 도구를 호출하여 확인한 뒤 답변하세요.
                        
                        사용자가 문을 열어도 되는지, 창문을 열어도 되는지,
                        환기해도 되는지를 질문하면 반드시 current_weather 도구를 호출하세요.
                        
                        사용자가 비, 바람, 외부 온도, 외부 습도를 언급하면
                        반드시 current_weather 도구를 호출하세요.
                        
                        현재 날씨 수치는 도구 결과에 존재하는 값만 사용하세요.
                        
                        도구를 호출하지 않은 경우 기온, 습도, 풍속, 강수 여부를 추측하거나 생성하지 마세요.
                        
                        현재 상태뿐 아니라 사용자가 앞으로 비가 올지, 곧 비가 올지,
                        이후 날씨가 어떻게 될지 묻는 경우 forecast_weather 도구를 반드시 호출하세요.
                        
                        실내 환경 문제의 해결책으로 환기, 문 개방 또는 창문 개방을 제안하려는 경우에는,
                        사용자가 환기를 직접 언급하지 않았더라도 반드시 current_weather 도구를 먼저 호출하세요.
                        
                        사용자에게 "외부 날씨를 확인하세요"라고 안내하지 마세요.
                        외부 날씨 확인이 필요하다면 당신이 직접 도구를 호출하여 확인 결과와 함께 실행 가능한 조치를 제시하세요.
                        
                        날씨 도구를 호출하지 않았다면 환기, 문 개방 또는 창문 개방을 조건부로라도 권장하지 마세요.
                        
                        도구 호출 결과를 근거로 답변을 생성하세요.
                        """)
                .defaultTools(currentWeatherTool, forecastWeatherTool)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @Bean
    public ChatClient sensorDataAnswerChatClient(@Qualifier(value = "googleGenAiChatModel") ChatModel geminiModel) {
        return ChatClient.builder(geminiModel)
                .defaultSystem("""
                        당신은 저장된 강의실 센서 측정값과 다른 서비스 API 조회 결과를 바탕으로 답변하는 어시스턴트입니다.
                        
                        저장된 센서 측정값, 과거 기록, 최근값, 평균, 최대/최소,
                        특정 기간의 통계나 추세 조회가 필요한 질문에 답변합니다.
                        
                        API 도구 결과에 없는 수치나 사실은 생성하지 마세요.
                        
                        아직 조회 API 도구가 제공되지 않은 경우,
                        센서 데이터 조회 도구가 아직 연결되지 않았다고 답변하세요.
                        """)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @Bean
    public ChatClient generalDataChatClient(@Qualifier(value = "googleGenAiChatModel") ChatModel geminiModel,
                                            SearchBuildingTool searchBuildingTool,
                                            SearchRoomTool searchRoomTool){
        return ChatClient.builder(geminiModel)
                .defaultSystem("""
                        당신은 팀 구조, 팀이 관리하는 건물, 건물 내 세부 목록 등 일반적인 조회 결과를 답변하는 어시스턴트입니다.
                        
                        팀이 관리하는 건물, 건물 내의 강의실, 강의실 내 기기와 센서, 사용자가 구독한 강의실 등 
                        DB에 저장된 데이터를 요구하는 질문에 다른 서비스의 API호출로 인한 데이터 제공을 통해 답변합니다.
                        
                        API 도구 호출 결과에 없는 수치나 사실은 생성하지 마세요.
                        """)
                .defaultTools(searchBuildingTool, searchRoomTool)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
