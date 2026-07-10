package com.nhnacademy.recommendation.tools;

import com.nhnacademy.recommendation.adaptor.EnvironmentClient;
import com.nhnacademy.recommendation.dto.llm.KmaCurrentWeatherResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CurrentWeatherTool {

    private final EnvironmentClient environmentClient;



    @Tool(
            name = "current_weather",
            description = """
                    실시간 날씨 정보를 조회합니다. (초단기실황조회)
                    반환되는 정보에는 기온, 습도, 강수 형태, 강수량, 풍속 등이 포함됩니다.
                    """
    )
    public KmaCurrentWeatherResponseDto getCurrentWeather(@ToolParam(description = "정보를 조회할 지역름 이름") String region){
        log.info("[Current Weather Tool] 실시간 날씨 조회 호출");

        return environmentClient.getNcst(region).getBody();
    }
}
