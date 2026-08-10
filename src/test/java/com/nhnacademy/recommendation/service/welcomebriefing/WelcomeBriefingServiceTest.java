package com.nhnacademy.recommendation.service.welcomebriefing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.dto.kma.KmaCurrentWeatherResponseDto;
import com.nhnacademy.recommendation.dto.kma.KmaForecastWeatherResponseDto;
import com.nhnacademy.recommendation.dto.room.RoomDetailResponse;
import com.nhnacademy.recommendation.dto.room.RoomDevicesResponse;
import com.nhnacademy.recommendation.dto.room.RoomRegionResponse;
import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingPolicyDto;
import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingResponse;
import com.nhnacademy.recommendation.exception.NotPositiveValueException;
import com.nhnacademy.recommendation.service.core.CoreRoomService;
import com.nhnacademy.recommendation.service.core.CoreWeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class WelcomeBriefingServiceTest {

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
    WelcomeBriefingPolicyService policyService;

    WelcomeBriefingService service;

    @BeforeEach
    void setUp() {
        service = new WelcomeBriefingService(
                chatClient,
                new ObjectMapper().findAndRegisterModules(),
                weatherService,
                coreRoomService,
                policyService
        );
    }

    @Test
    @DisplayName("더미 실내 환경 분석 결과와 조회 데이터를 조합해 LLM 웰컴 브리핑을 생성한다")
    void generateWelcomeBriefing() {
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
                "CO2 상승으로 환기가 필요합니다.",
                "현재 CO2가 빠르게 상승 중입니다.",
                "비와 강풍 가능성이 있어 창문 개방은 주의가 필요합니다.",
                List.of("환기장치를 점검하세요."),
                List.of("센서 수신 상태를 확인하세요.")
        );

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
        verify(requestSpec).user(org.mockito.ArgumentMatchers.<String>argThat(prompt ->
                prompt.contains("\"indoorEnvironmentAnalysis\"")
                        && prompt.contains("\"primaryLabel\":\"CO2_RISING_FAST\"")
                        && prompt.contains("\"todayWeatherOutlook\"")
                        && prompt.contains("비가 예상되어 창문 개방 환기는 주의가 필요합니다.")
                        && prompt.contains("강풍 가능성이 있어 창문 개방을 피하는 것이 좋습니다.")
                        && prompt.contains("\"deviceName\":\"환기장치\"")
        ));
    }

    @Test
    @DisplayName("teamId가 양수가 아니면 웰컴 브리핑 생성을 중단한다")
    void generateWelcomeBriefing_InvalidTeamId() {
        assertThatThrownBy(() -> service.generateWelcomeBriefing(0L, 10L))
                .isInstanceOf(NotPositiveValueException.class);

        verifyNoInteractions(coreRoomService, weatherService, policyService, chatClient);
    }

    @Test
    @DisplayName("roomId가 양수가 아니면 웰컴 브리핑 생성을 중단한다")
    void generateWelcomeBriefing_InvalidRoomId() {
        assertThatThrownBy(() -> service.generateWelcomeBriefing(3L, 0L))
                .isInstanceOf(NotPositiveValueException.class);

        verifyNoInteractions(coreRoomService, weatherService, policyService, chatClient);
    }
}
