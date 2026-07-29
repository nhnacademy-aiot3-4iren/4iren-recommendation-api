package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.dto.kma.KmaCurrentWeatherResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CurrentWeatherTool {

    private final CoreClient coreClient;



    @Tool(
            name = "current_weather",
            description = """
                    지정된 지역의 현재 기온, 습도, 강수 형태, 강수량,
                            풍속을 조회합니다.
                            환기 여부, 문 개방 여부, 창문 개방 여부 또는
                            외부 공기 유입 가능 여부를 판단할 때 반드시 호출하세요.
                            사용자가 비, 강수, 바람, 외부 온도 또는 외부 습도를
                            언급한 경우에도 반드시 호출하세요.
                            이 도구를 호출하지 않은 상태에서 현재 날씨 수치를
                            생성하거나 추측해서는 안 됩니다.
                    """
    )
    public KmaCurrentWeatherResponseDto getCurrentWeather(@ToolParam(description = "정보를 조회할 지역 이름") String region){
        long start = System.currentTimeMillis();
        log.info("[Current Weather Tool] 실시간 날씨 조회 호출");

        try {
            return coreClient.getNcst(region).getBody();
        } finally {
            log.info("[Timing][Tool] current_weather region={} elapsed={}ms", region, System.currentTimeMillis() - start);
        }
    }
}
