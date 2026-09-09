package com.nhnacademy.recommendation.service.dailysummary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.dto.dailysummary.DailySummaryContext;
import com.nhnacademy.recommendation.dto.dailysummary.DailySummaryResponse;
import com.nhnacademy.recommendation.dto.kma.KmaWeatherHistoryResponseDto;
import com.nhnacademy.recommendation.dto.room.RoomDetailResponse;
import com.nhnacademy.recommendation.dto.room.RoomRegionResponse;
import com.nhnacademy.recommendation.dto.sensor.SensorMetricSeriesResponse;
import com.nhnacademy.recommendation.exception.NotPositiveValueException;
import com.nhnacademy.recommendation.exception.RequiredValueException;
import com.nhnacademy.recommendation.service.DailyWeatherCacheService;
import com.nhnacademy.recommendation.service.core.CoreRoomService;
import com.nhnacademy.recommendation.service.core.CoreSensorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DailySummaryServiceTest {

    @Mock
    ChatClient chatClient;

    @Mock
    ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    CoreRoomService coreRoomService;

    @Mock
    CoreSensorService coreSensorService;

    @Mock
    DailyWeatherCacheService dailyWeatherCacheService;

    ObjectMapper objectMapper;
    DailySummaryService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new DailySummaryService(
                chatClient,
                objectMapper,
                coreRoomService,
                coreSensorService,
                dailyWeatherCacheService,
                Clock.fixed(Instant.parse("2026-08-27T09:00:00Z"), java.time.ZoneId.of("Asia/Seoul"))
        );
    }

    @Test
    @DisplayName("강의실 센서 시계열과 외부 날씨 히스토리를 조합해 하루 요약을 생성한다")
    void generateDailySummary() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 20);
        DailySummaryResponse expected = new DailySummaryResponse(
                "오후로 갈수록 실내 온도와 외부 온도가 함께 높았습니다.",
                "실내 온도는 25.0도에서 27.0도까지 상승했습니다.",
                "외부 온도는 30.4도에서 34.3도까지 상승했습니다.",
                "외부가 더워 창문 환기는 신중하게 판단해야 합니다.",
                List.of("CO2 추이를 추가 확인하세요."),
                List.of("18시 외부 날씨 데이터가 누락되었습니다.")
        );
        RoomDetailResponse room = new RoomDetailResponse(10L, 100L, "본관", "101호", "실습실", 0L, 0L);
        RoomRegionResponse region = new RoomRegionResponse(10L, "광주 동구 서석동");
        SensorMetricSeriesResponse sensorSeries = sensorSeries(date);
        KmaWeatherHistoryResponseDto weatherHistory = weatherHistory(date);

        given(coreRoomService.getRoomDetailInternal(10L)).willReturn(room);
        given(coreRoomService.getRoomRegion(10L)).willReturn(region);
        given(coreSensorService.getSensorMetricSeriesInternal(
                10L,
                Instant.parse("2026-08-20T00:00:00Z"),
                Instant.parse("2026-08-20T09:00:00Z"),
                Duration.ofHours(1)
        )).willReturn(sensorSeries);
        given(dailyWeatherCacheService.getDailyWeather("광주 동구 서석동", date, 9, 18))
                .willReturn(weatherHistory);
        given(chatClient.prompt()).willReturn(requestSpec);
        given(requestSpec.user(anyString())).willReturn(requestSpec);
        given(requestSpec.call()).willReturn(callResponseSpec);
        given(callResponseSpec.entity(DailySummaryResponse.class)).willReturn(expected);

        DailySummaryResponse result = service.generateDailySummary(3L, 10L, date, 9, 18);

        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<String> contextCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(contextCaptor.capture());
        DailySummaryContext context = objectMapper.readValue(contextCaptor.getValue(), DailySummaryContext.class);
        assertThat(context.teamId()).isEqualTo(3L);
        assertThat(context.roomId()).isEqualTo(10L);
        assertThat(context.date()).isEqualTo(date);
        assertThat(context.analysisPeriod().timezone()).isEqualTo("Asia/Seoul");
        assertThat(context.analysisPeriod().interval()).isEqualTo("PT1H");
        assertThat(context.indoorSensorSeries()).isEqualTo(sensorSeries);
        assertThat(context.outdoorWeatherHistory()).isEqualTo(weatherHistory);
    }

    @Test
    @DisplayName("teamId가 양수가 아니면 하루 요약 생성을 중단한다")
    void generateDailySummary_InvalidTeamId() {
        assertThatThrownBy(() -> service.generateDailySummary(0L, 10L, LocalDate.of(2026, 8, 20), 9, 18))
                .isInstanceOf(NotPositiveValueException.class);

        verifyNoInteractions(coreRoomService, coreSensorService, dailyWeatherCacheService, chatClient);
    }

    @Test
    @DisplayName("date가 없으면 하루 요약 생성을 중단한다")
    void generateDailySummary_RequiredDate() {
        assertThatThrownBy(() -> service.generateDailySummary(3L, 10L, null, 9, 18))
                .isInstanceOf(RequiredValueException.class);

        verifyNoInteractions(coreRoomService, coreSensorService, dailyWeatherCacheService, chatClient);
    }

    @Test
    @DisplayName("분석 시작 시간이 종료 시간보다 늦거나 같으면 하루 요약 생성을 중단한다")
    void generateDailySummary_InvalidHourRange() {
        assertThatThrownBy(() -> service.generateDailySummary(3L, 10L, LocalDate.of(2026, 8, 20), 18, 9))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("startHour는 endHour보다 작아야 합니다.");

        verifyNoInteractions(coreRoomService, coreSensorService, dailyWeatherCacheService, chatClient);
    }

    @Test
    @DisplayName("오늘 하루 요약을 18시 20분 전에 요청하면 생성을 중단한다")
    void generateDailySummary_TodayBeforeAvailableTime() {
        DailySummaryService before18Service = new DailySummaryService(
                chatClient,
                objectMapper,
                coreRoomService,
                coreSensorService,
                dailyWeatherCacheService,
                Clock.fixed(Instant.parse("2026-08-20T09:19:00Z"), java.time.ZoneId.of("Asia/Seoul"))
        );

        assertThatThrownBy(() -> before18Service.generateDailySummary(3L, 10L, LocalDate.of(2026, 8, 20), 9, 18))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("오늘 하루 요약은 18시 20분 이후에 생성할 수 있습니다.");

        verifyNoInteractions(coreRoomService, coreSensorService, dailyWeatherCacheService, chatClient);
    }

    @Test
    @DisplayName("미래 날짜 하루 요약을 요청하면 생성을 중단한다")
    void generateDailySummary_FutureDate() {
        assertThatThrownBy(() -> service.generateDailySummary(3L, 10L, LocalDate.of(2026, 8, 28), 9, 18))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("미래 날짜의 하루 요약은 생성할 수 없습니다.");

        verifyNoInteractions(coreRoomService, coreSensorService, dailyWeatherCacheService, chatClient);
    }

    private SensorMetricSeriesResponse sensorSeries(LocalDate date) {
        return new SensorMetricSeriesResponse(
                10L,
                Instant.parse("2026-08-20T00:00:00Z"),
                Instant.parse("2026-08-20T09:00:00Z"),
                Duration.ofHours(1),
                List.of(
                        new SensorMetricSeriesResponse.Sensor(
                                "24e124128c140101",
                                List.of(new SensorMetricSeriesResponse.Metric(
                                        "temperature",
                                        "온도",
                                        "GAUGE",
                                        "실내 공기의 섭씨 온도",
                                        "Cel",
                                        "섭씨",
                                        "°C",
                                        List.of(
                                                new SensorMetricSeriesResponse.Point(
                                                        date.atTime(10, 0).atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant(),
                                                        25.0
                                                ),
                                                new SensorMetricSeriesResponse.Point(
                                                        date.atTime(18, 0).atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant(),
                                                        27.0
                                                )
                                        )
                                ))
                        )
                )
        );
    }

    private KmaWeatherHistoryResponseDto weatherHistory(LocalDate date) {
        return new KmaWeatherHistoryResponseDto(
                "광주 동구 서석동",
                "전남광주통합특별시 동구 서남동",
                date,
                new KmaWeatherHistoryResponseDto.AnalysisPeriod(LocalTime.of(9, 0), LocalTime.of(18, 0)),
                10,
                9,
                false,
                List.of(LocalDateTime.of(2026, 8, 20, 18, 0)),
                List.of(
                        new KmaWeatherHistoryResponseDto.WeatherSnapshot(
                                LocalDateTime.of(2026, 8, 20, 9, 0),
                                30.4,
                                79,
                                "NONE",
                                0.0,
                                1.4
                        ),
                        new KmaWeatherHistoryResponseDto.WeatherSnapshot(
                                LocalDateTime.of(2026, 8, 20, 13, 0),
                                34.3,
                                67,
                                "NONE",
                                0.0,
                                1.4
                        )
                )
        );
    }
}
