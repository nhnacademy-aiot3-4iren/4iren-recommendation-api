package com.nhnacademy.recommendation.service.welcomebriefing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.dto.kma.KmaCurrentWeatherResponseDto;
import com.nhnacademy.recommendation.dto.kma.KmaForecastWeatherResponseDto;
import com.nhnacademy.recommendation.dto.room.RoomDetailResponse;
import com.nhnacademy.recommendation.dto.room.RoomDevicesResponse;
import com.nhnacademy.recommendation.dto.room.RoomRegionResponse;
import com.nhnacademy.recommendation.dto.sensor.SensorMetricSummaryResponse;
import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingContext;
import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingMlRecommendation;
import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingPolicyDto;
import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingResponse;
import com.nhnacademy.recommendation.exception.ModelServingException;
import com.nhnacademy.recommendation.exception.NotPositiveValueException;
import com.nhnacademy.recommendation.model.behavior.BehaviorRecommendation;
import com.nhnacademy.recommendation.service.behavior.BehaviorRecommendationService;
import com.nhnacademy.recommendation.service.core.CoreRoomService;
import com.nhnacademy.recommendation.service.core.CoreSensorService;
import com.nhnacademy.recommendation.service.core.CoreWeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class WelcomeBriefingServiceTest {

    private static final ZoneId ASIA_SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalDate PREDICTION_DATE = LocalDate.of(2026, 8, 11);

    @Mock
    ChatClient chatClient;

    @Mock
    ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    CoreWeatherService weatherService;

    @Mock
    CoreRoomService coreRoomService;

    @Mock
    CoreSensorService coreSensorService;

    @Mock
    WelcomeBriefingPolicyService policyService;

    @Mock
    BehaviorRecommendationService behaviorRecommendationService;

    @Mock
    ObjectProvider<BehaviorRecommendationService> behaviorRecommendationServiceProvider;

    ObjectMapper objectMapper;

    WelcomeBriefingService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new WelcomeBriefingService(
                chatClient,
                objectMapper,
                weatherService,
                coreRoomService,
                coreSensorService,
                policyService,
                behaviorRecommendationServiceProvider,
                Clock.fixed(Instant.parse("2026-08-10T15:30:00Z"), ASIA_SEOUL)
        );
    }

    @Test
    @DisplayName("현재 센서 데이터와 ML 추천 스케줄, 조회 데이터를 조합해 LLM 웰컴 브리핑을 생성한다")
    void generateWelcomeBriefing() throws Exception {
        RoomDetailResponse room = new RoomDetailResponse(10L, 100L, "본관", "101호", "실습실", 0L, 0L);
        RoomRegionResponse region = new RoomRegionResponse(10L, "광주");
        RoomDevicesResponse devices = new RoomDevicesResponse(
                10L,
                "101호",
                List.of(new RoomDevicesResponse.DeviceSummary(1L, "환기장치"))
        );
        KmaCurrentWeatherResponseDto currentWeather = new KmaCurrentWeatherResponseDto(
                "광주",
                "광주",
                58,
                74,
                "2026-08-10 08:00",
                "28.0",
                "없음",
                "0mm",
                "75",
                "180",
                "4.0",
                "0",
                "0"
        );
        KmaForecastWeatherResponseDto forecastWeather = new KmaForecastWeatherResponseDto(
                "광주",
                "광주",
                58,
                74,
                "2026-08-10 08:00",
                List.of(
                        new KmaForecastWeatherResponseDto.Forecast(
                                "2026-08-10 09:00",
                                "흐림",
                                "없음",
                                "0mm",
                                "40",
                                "29.0",
                                "75",
                                "180",
                                "4.0",
                                "0",
                                "0",
                                "0"
                        ),
                        new KmaForecastWeatherResponseDto.Forecast(
                                "2026-08-10 15:00",
                                "비",
                                "비",
                                "1mm",
                                "70",
                                "31.0",
                                "80",
                                "180",
                                "9.0",
                                "0",
                                "0",
                                "0"
                        )
                )
        );
        WelcomeBriefingResponse expected = new WelcomeBriefingResponse(
                "현재 CO2 상태와 오늘 기기 운전 스케줄을 확인해야 합니다.",
                "현재 CO2는 980ppm입니다.",
                "비와 강풍 가능성이 있어 창문 개방은 주의가 필요합니다.",
                List.of("12:00~12:30 환기장치 사용을 검토하세요."),
                List.of("센서 수신 상태를 확인하세요.")
        );
        BehaviorRecommendation behaviorRecommendation = behaviorRecommendation();

        given(behaviorRecommendationServiceProvider.getIfAvailable()).willReturn(behaviorRecommendationService);
        given(behaviorRecommendationService.recommend(PREDICTION_DATE, 10L)).willReturn(behaviorRecommendation);
        given(coreSensorService.getSensorMetricSummaryInternal(10L)).willReturn(sensorMetricSummary());
        given(coreRoomService.getRoomDetailInternal(10L)).willReturn(room);
        given(coreRoomService.getRoomRegion(10L)).willReturn(region);
        given(coreRoomService.getRoomDevices(10L)).willReturn(devices);
        given(policyService.getPolicyOrDefault(3L, 10L))
                .willReturn(new WelcomeBriefingPolicyDto(30, 60, 8.0, 70, true));
        given(weatherService.getCurrentWeather("광주")).willReturn(currentWeather);
        given(weatherService.getForecastWeather("광주")).willReturn(forecastWeather);
        given(chatClient.prompt()).willReturn(requestSpec);
        given(requestSpec.user(anyString())).willReturn(requestSpec);
        given(requestSpec.call()).willReturn(callResponseSpec);
        given(callResponseSpec.entity(WelcomeBriefingResponse.class)).willReturn(expected);

        WelcomeBriefingResponse result = service.generateWelcomeBriefing(3L, 10L);

        assertThat(result).isEqualTo(expected);
        verify(behaviorRecommendationService).recommend(PREDICTION_DATE, 10L);

        ArgumentCaptor<String> contextCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(contextCaptor.capture());
        WelcomeBriefingContext context = objectMapper.readValue(
                contextCaptor.getValue(),
                WelcomeBriefingContext.class
        );

        assertThat(context.room().location()).isEqualTo("회의실");
        assertThat(context.mlRecommendation()).isEqualTo(welcomeBriefingMlRecommendation());
        assertThat(context.currentSensor().temperatureC()).isEqualTo(25.0);
        assertThat(context.currentSensor().humidityPercent()).isEqualTo(42.0);
        assertThat(context.currentSensor().co2Ppm()).isEqualTo(980.0);
        assertThat(context.currentSensor().dataSufficient()).isTrue();
        assertThat(context.todayWeatherOutlook().cautions()).containsExactly(
                "비가 예상되어 창문 개방 환기는 주의가 필요합니다.",
                "강풍 가능성이 있어 창문 개방을 피하는 것이 좋습니다.",
                "외부 습도가 높을 수 있어 환기 후 실내 습도 확인이 필요합니다."
        );
        assertThat(context.devices()).singleElement()
                .satisfies(device -> assertThat(device.deviceName()).isEqualTo("환기장치"));
    }

    @Test
    @DisplayName("Behavior 추천 실패를 고정 스케줄로 숨기지 않고 그대로 전파한다")
    void generateWelcomeBriefing_BehaviorRecommendationFailure() {
        ModelServingException failure = new ModelServingException("ONNX inference failed");
        given(behaviorRecommendationServiceProvider.getIfAvailable()).willReturn(behaviorRecommendationService);
        given(behaviorRecommendationService.recommend(PREDICTION_DATE, 10L)).willThrow(failure);

        assertThatThrownBy(() -> service.generateWelcomeBriefing(3L, 10L))
                .isSameAs(failure);

        verify(behaviorRecommendationService).recommend(PREDICTION_DATE, 10L);
        verifyNoInteractions(coreSensorService, coreRoomService, weatherService, policyService, chatClient);
    }

    @Test
    @DisplayName("Model serving이 비활성화되면 fake 추천 없이 명확하게 실패한다")
    void generateWelcomeBriefing_ModelServingDisabled() {
        assertThatThrownBy(() -> service.generateWelcomeBriefing(3L, 10L))
                .isInstanceOf(ModelServingException.class)
                .hasMessage("Model serving이 비활성화되어 Behavior 추천을 생성할 수 없습니다.");

        verify(behaviorRecommendationServiceProvider).getIfAvailable();
        verifyNoInteractions(
                behaviorRecommendationService,
                coreSensorService,
                coreRoomService,
                weatherService,
                policyService,
                chatClient
        );
    }

    @Test
    @DisplayName("teamId가 양수가 아니면 웰컴 브리핑 생성을 중단한다")
    void generateWelcomeBriefing_InvalidTeamId() {
        assertThatThrownBy(() -> service.generateWelcomeBriefing(0L, 10L))
                .isInstanceOf(NotPositiveValueException.class);

        verifyNoInteractions(
                behaviorRecommendationServiceProvider,
                behaviorRecommendationService,
                coreSensorService,
                coreRoomService,
                weatherService,
                policyService,
                chatClient
        );
    }

    @Test
    @DisplayName("roomId가 양수가 아니면 웰컴 브리핑 생성을 중단한다")
    void generateWelcomeBriefing_InvalidRoomId() {
        assertThatThrownBy(() -> service.generateWelcomeBriefing(3L, 0L))
                .isInstanceOf(NotPositiveValueException.class);

        verifyNoInteractions(
                behaviorRecommendationServiceProvider,
                behaviorRecommendationService,
                coreSensorService,
                coreRoomService,
                weatherService,
                policyService,
                chatClient
        );
    }

    @Test
    @DisplayName("10시 이후에는 웰컴 브리핑 생성을 중단한다")
    void generateWelcomeBriefing_AfterCutoffTime() {
        WelcomeBriefingService afterCutoffService = new WelcomeBriefingService(
                chatClient,
                objectMapper,
                weatherService,
                coreRoomService,
                coreSensorService,
                policyService,
                behaviorRecommendationServiceProvider,
                Clock.fixed(Instant.parse("2026-08-11T01:00:00Z"), ASIA_SEOUL)
        );

        assertThatThrownBy(() -> afterCutoffService.generateWelcomeBriefing(3L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("웰컴 브리핑은 10시 전까지만 생성할 수 있습니다.");

        verifyNoInteractions(
                behaviorRecommendationServiceProvider,
                behaviorRecommendationService,
                coreSensorService,
                coreRoomService,
                weatherService,
                policyService,
                chatClient
        );
    }

    private BehaviorRecommendation behaviorRecommendation() {
        return new BehaviorRecommendation(
                "4iren.behavior.recommendation.v1",
                new BehaviorRecommendation.Context(
                        PREDICTION_DATE,
                        DayOfWeek.TUESDAY,
                        10L,
                        "회의실",
                        "Asia/Seoul"
                ),
                "DAILY_DEVICE_USAGE_SCHEDULE",
                List.of(
                        new BehaviorRecommendation.ScheduleItem(
                                "HEATER",
                                "ON",
                                LocalTime.of(6, 15),
                                LocalTime.of(8, 45),
                                0.8123
                        ),
                        new BehaviorRecommendation.ScheduleItem(
                                "VENTILATION",
                                "ON",
                                LocalTime.of(14, 15),
                                LocalTime.of(14, 45),
                                0.7312
                        ),
                        new BehaviorRecommendation.ScheduleItem(
                                "HEATER",
                                "OFF",
                                LocalTime.of(8, 45),
                                null,
                                0.6543
                        )
                )
        );
    }

    private WelcomeBriefingMlRecommendation welcomeBriefingMlRecommendation() {
        return new WelcomeBriefingMlRecommendation(
                "4iren.behavior.recommendation.v1",
                new WelcomeBriefingMlRecommendation.Context(
                        PREDICTION_DATE,
                        DayOfWeek.TUESDAY,
                        10L,
                        "회의실",
                        "Asia/Seoul"
                ),
                "DAILY_DEVICE_USAGE_SCHEDULE",
                List.of(
                        new WelcomeBriefingMlRecommendation.RecommendedSchedule(
                                "HEATER",
                                "ON",
                                LocalTime.of(6, 15),
                                LocalTime.of(8, 45),
                                0.8123
                        ),
                        new WelcomeBriefingMlRecommendation.RecommendedSchedule(
                                "VENTILATION",
                                "ON",
                                LocalTime.of(14, 15),
                                LocalTime.of(14, 45),
                                0.7312
                        ),
                        new WelcomeBriefingMlRecommendation.RecommendedSchedule(
                                "HEATER",
                                "OFF",
                                LocalTime.of(8, 45),
                                null,
                                0.6543
                        )
                )
        );
    }

    private SensorMetricSummaryResponse sensorMetricSummary() {
        return new SensorMetricSummaryResponse(
                10L,
                Instant.parse("2026-08-10T23:00:00Z"),
                Duration.ofMinutes(15),
                List.of(
                        new SensorMetricSummaryResponse.Metric(
                                "temperature",
                                "온도",
                                "GAUGE",
                                "실내 공기의 섭씨 온도",
                                25.0,
                                "Cel",
                                "섭씨",
                                "°C"
                        ),
                        new SensorMetricSummaryResponse.Metric(
                                "humidity",
                                "상대습도",
                                "GAUGE",
                                "실내 공기의 상대습도",
                                42.0,
                                "%",
                                "퍼센트",
                                "%"
                        ),
                        new SensorMetricSummaryResponse.Metric(
                                "co2",
                                "이산화탄소 농도",
                                "GAUGE",
                                "실내 공기 중 이산화탄소 농도",
                                980.0,
                                "[ppm]",
                                "백만분율",
                                "ppm"
                        )
                )
        );
    }
}
