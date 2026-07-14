package com.nhnacademy.recommendation.tools;


import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.dto.llm.KmaForecastWeatherResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ForecastWeatherTool {

    private final CoreClient coreClient;


    @Tool(name = "forecast_weather",
            description = """
                    오늘 날씨 예보를 조회합니다. (현재 시각으로부터 6시간 뒤까지 1시간 마다)
                    반환되는 정보에는 기온, 습도, 강수 형태, 강수량, 풍속 등이 포함됩니다.
                    """)
    public KmaForecastWeatherResponseDto getTodayForecastWeather(@ToolParam(description = "정보를 조회할 지역 이름") String region) {
        log.info("[Forecast Weather Tool] 오늘 날씨 예보 호출");

        return coreClient.getFcst(region).getBody();

    }


}
