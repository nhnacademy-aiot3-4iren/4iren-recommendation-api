package com.nhnacademy.recommendation.service;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.dto.kma.KmaCurrentWeatherResponseDto;
import com.nhnacademy.recommendation.dto.kma.KmaForecastWeatherResponseDto;
import com.nhnacademy.recommendation.exception.RequiredValueException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CoreWeatherServiceTest {

    @Mock
    CoreClient coreClient;

    CoreWeatherService service;

    @BeforeEach
    void setUp() {
        service = new CoreWeatherService(coreClient);
    }

    @Test
    @DisplayName("현재 날씨 조회 성공")
    void getCurrentWeather() {
        KmaCurrentWeatherResponseDto weather = new KmaCurrentWeatherResponseDto("요청지역", "지역", 25, 69, "2026-08-05 13:00", "30C", "없음", "0mm", "55%", "23deg", "1.3m/s", "-0.4m/s", "-1.1m/s");
        given(coreClient.getNcst("지역")).willReturn(ResponseEntity.ok(weather));

        KmaCurrentWeatherResponseDto result = service.getCurrentWeather("지역");

        assertThat(result).isEqualTo(weather);
    }

    @Test
    @DisplayName("날씨 예보 조회 성공")
    void getForecastWeather() {
        KmaForecastWeatherResponseDto weather = new KmaForecastWeatherResponseDto(null, null, null, null, null, null);
        given(coreClient.getFcst("지역")).willReturn(ResponseEntity.ok(weather));

        KmaForecastWeatherResponseDto result = service.getForecastWeather("지역");

        assertThat(result).isEqualTo(weather);
    }

    @Test
    @DisplayName("현재 날씨 조회 실패 - 지역명 누락")
    void getCurrentWeather_Fail_RequiredValue() {
        assertThatThrownBy(() -> service.getCurrentWeather(" "))
                .isInstanceOf(RequiredValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("날씨 예보 조회 실패 - CoreClient 예외 전파")
    void getForecastWeather_Fail_CoreClient() {
        RuntimeException exception = new RuntimeException("core api error");
        given(coreClient.getFcst("지역")).willThrow(exception);

        assertThatThrownBy(() -> service.getForecastWeather("지역"))
                .isSameAs(exception);
    }
}
