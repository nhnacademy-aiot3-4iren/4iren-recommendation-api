package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.dto.kma.KmaForecastWeatherResponseDto;
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
class ForecastWeatherToolTest {
    @Mock
    CoreClient coreClient;

    @Test
    @DisplayName("날씨 예보 조회")
    void getTodayForecastWeather(){
        ForecastWeatherTool tool = new ForecastWeatherTool(coreClient);
        KmaForecastWeatherResponseDto responseDto = new KmaForecastWeatherResponseDto(null, null, null,null,null,null);
        given(coreClient.getFcst("지역이름")).willReturn(ResponseEntity.ok(responseDto));

        ToolResult<KmaForecastWeatherResponseDto> result = tool.getTodayForecastWeather("지역이름");

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo("SUCCESS");
        assertThat(result.data()).isEqualTo(responseDto);
    }

    @Test
    @DisplayName("날씨 예보 조회 실패 - CoreAPI 오류")
    void getTodayForecastWeather_Fail(){
        ForecastWeatherTool tool = new ForecastWeatherTool(coreClient);
        given(coreClient.getFcst("지역이름")).willThrow(new RuntimeException());

        ToolResult<KmaForecastWeatherResponseDto> result = tool.getTodayForecastWeather("지역이름");

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("FORECAST_WEATHER_QUERY_FAILED");
        assertThat(result.data()).isNull();
    }
}
