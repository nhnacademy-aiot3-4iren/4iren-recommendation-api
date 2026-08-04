package com.nhnacademy.recommendation.tools.general;


import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.dto.kma.KmaCurrentWeatherResponseDto;
import com.nhnacademy.recommendation.dto.tool.ToolResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
class CurrentWeatherToolTest {
    @Mock
    CoreClient coreClient;

    @Test
    @DisplayName("실시간 날씨 조회(초단기실황조회)")
    void getCurrentWeather() {
        CurrentWeatherTool tool = new CurrentWeatherTool(coreClient);

        KmaCurrentWeatherResponseDto responseDto = new KmaCurrentWeatherResponseDto("요청한 지역이름", "지역이름", 25, 69, "2026-08-04 13:00", "36.4℃", "없음", "0mm", "55%", "23deg", "1.3m/s", "-0.4m/s", "-1.1m/s");

        ResponseEntity<KmaCurrentWeatherResponseDto> currentWeather = ResponseEntity.ok(responseDto);

        given(coreClient.getNcst("지역이름")).willReturn(currentWeather);

        ToolResult<KmaCurrentWeatherResponseDto> result = tool.getCurrentWeather("지역이름");

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo("SUCCESS");
        assertThat(result.data()).isEqualTo(responseDto);
    }

    @Test
    @DisplayName("실시간 날씨 조회(초단기실황조회) 실패 - CoreAPI 오류")
    void getCurrentWeather_Fail() {
        CurrentWeatherTool tool = new CurrentWeatherTool(coreClient);


        given(coreClient.getNcst("지역이름")).willThrow(new RuntimeException());

        ToolResult<KmaCurrentWeatherResponseDto> result = tool.getCurrentWeather("지역이름");

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("CURRENT_WEATHER_QUERY_FAILED");
        assertThat(result.data()).isNull();
    }

}
