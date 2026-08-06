package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.dto.kma.KmaForecastWeatherResponseDto;
import com.nhnacademy.recommendation.dto.tool.ToolResult;
import com.nhnacademy.recommendation.service.core.CoreWeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ForecastWeatherToolTest {
    @Mock
    CoreWeatherService coreWeatherService;

    ForecastWeatherTool tool;

    @BeforeEach
    void setUp() {
        tool = new ForecastWeatherTool(coreWeatherService);
    }

    @Test
    @DisplayName("날씨 예보 조회")
    void getTodayForecastWeather(){
        KmaForecastWeatherResponseDto responseDto = new KmaForecastWeatherResponseDto(null, null, null,null,null,null);
        given(coreWeatherService.getForecastWeather("지역이름")).willReturn(responseDto);

        ToolResult<KmaForecastWeatherResponseDto> result = tool.getTodayForecastWeather("지역이름");

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo("SUCCESS");
        assertThat(result.data()).isEqualTo(responseDto);
    }

    @Test
    @DisplayName("날씨 예보 조회 실패 - CoreAPI 오류")
    void getTodayForecastWeather_Fail(){
        given(coreWeatherService.getForecastWeather("지역이름")).willThrow(new RuntimeException());

        ToolResult<KmaForecastWeatherResponseDto> result = tool.getTodayForecastWeather("지역이름");

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("FORECAST_WEATHER_QUERY_FAILED");
        assertThat(result.data()).isNull();
    }
}
