package com.nhnacademy.recommendation.tools.general;


import com.nhnacademy.recommendation.dto.kma.KmaForecastWeatherResponseDto;
import com.nhnacademy.recommendation.dto.tool.ToolResult;
import com.nhnacademy.recommendation.service.CoreWeatherService;
import com.nhnacademy.recommendation.util.TimingLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ForecastWeatherTool {

    private final CoreWeatherService coreWeatherService;


    @Tool(name = "forecast_weather",
            description = """
                    오늘 날씨 예보를 조회합니다. (현재 시각으로부터 6시간 뒤까지 1시간 마다)
                    반환되는 정보에는 기온, 습도, 강수 형태, 강수량, 풍속 등이 포함됩니다.
                    """)
    public ToolResult<KmaForecastWeatherResponseDto> getTodayForecastWeather(@ToolParam(description = "정보를 조회할 지역 이름") String region) {
        log.info("[Forecast Weather Tool] 오늘 날씨 예보 호출");
        return TimingLog.measure(log, "[Timing][Tool] forecast_weather region=" + region, () -> {
            try {
                return ToolResult.success(coreWeatherService.getForecastWeather(region));
            } catch (Exception e) {
                log.warn("[ForecastWeatherTool] 날씨 예보 조회 실패. region={}", region, e);
                return ToolResult.failure("FORECAST_WEATHER_QUERY_FAILED", "날씨 예보를 조회하지 못했습니다.");
            }
        });
    }


}
