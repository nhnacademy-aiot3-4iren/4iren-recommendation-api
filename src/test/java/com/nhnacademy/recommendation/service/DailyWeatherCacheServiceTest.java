package com.nhnacademy.recommendation.service;

import com.nhnacademy.recommendation.dto.kma.KmaWeatherHistoryResponseDto;
import com.nhnacademy.recommendation.service.core.CoreWeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DailyWeatherCacheServiceTest {

    @Mock
    CoreWeatherService coreWeatherService;

    DailyWeatherCacheService service;

    @BeforeEach
    void setUp() {
        service = new DailyWeatherCacheService(coreWeatherService);
    }

    @Test
    @DisplayName("하루 외부 날씨 조회 - 지정 시간대")
    void getDailyWeather() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        KmaWeatherHistoryResponseDto weather = weather(date);
        given(coreWeatherService.getWeatherHistory("지역", date, 10, 17)).willReturn(weather);

        KmaWeatherHistoryResponseDto result = service.getDailyWeather("지역", date, 10, 17);

        assertThat(result).isEqualTo(weather);
        verify(coreWeatherService).getWeatherHistory("지역", date, 10, 17);
    }

    @Test
    @DisplayName("하루 외부 날씨 조회 - 기본 시간대")
    void getDailyWeather_DefaultHours() {
        LocalDate date = LocalDate.of(2026, 8, 20);
        KmaWeatherHistoryResponseDto weather = weather(date);
        given(coreWeatherService.getWeatherHistory("지역", date, 9, 18)).willReturn(weather);

        KmaWeatherHistoryResponseDto result = service.getDailyWeather("지역", date);

        assertThat(result).isEqualTo(weather);
        verify(coreWeatherService).getWeatherHistory("지역", date, 9, 18);
    }

    private KmaWeatherHistoryResponseDto weather(LocalDate date) {
        return new KmaWeatherHistoryResponseDto(
                "요청지역",
                "지역",
                date,
                null,
                10,
                10,
                true,
                List.of(),
                List.of()
        );
    }
}
