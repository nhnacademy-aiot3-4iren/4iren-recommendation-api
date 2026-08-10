package com.nhnacademy.recommendation.service.welcomebriefing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.dto.kma.KmaCurrentWeatherResponseDto;
import com.nhnacademy.recommendation.dto.kma.KmaForecastWeatherResponseDto;
import com.nhnacademy.recommendation.dto.room.RoomDetailResponse;
import com.nhnacademy.recommendation.dto.room.RoomDevicesResponse;
import com.nhnacademy.recommendation.dto.room.RoomRegionResponse;
import com.nhnacademy.recommendation.dto.welcomebriefing.CurrentWeatherSnapshot;
import com.nhnacademy.recommendation.dto.welcomebriefing.DeviceStatus;
import com.nhnacademy.recommendation.dto.welcomebriefing.IndoorEnvironmentAnalysis;
import com.nhnacademy.recommendation.dto.welcomebriefing.RoomInfo;
import com.nhnacademy.recommendation.dto.welcomebriefing.TodayWeatherOutlook;
import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingContext;
import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingResponse;
import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingPolicyDto;
import com.nhnacademy.recommendation.service.core.CoreRequestValidator;
import com.nhnacademy.recommendation.service.core.CoreRoomService;
import com.nhnacademy.recommendation.service.core.CoreWeatherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WelcomeBriefingService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final CoreWeatherService weatherService;
    private final CoreRoomService coreRoomService;
    private final WelcomeBriefingPolicyService welcomeBriefingPolicyService;

    public WelcomeBriefingService(@Qualifier("welcomeBriefingChatClient") ChatClient chatClient,
                                  ObjectMapper objectMapper,
                                  CoreWeatherService weatherService,
                                  CoreRoomService coreRoomService,
                                  WelcomeBriefingPolicyService welcomeBriefingPolicyService) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.weatherService = weatherService;
        this.coreRoomService = coreRoomService;
        this.welcomeBriefingPolicyService = welcomeBriefingPolicyService;
    }

    public WelcomeBriefingResponse generateWelcomeBriefing(Long teamId, Long roomId) {
        CoreRequestValidator.requirePositive(teamId, "teamId");
        CoreRequestValidator.requirePositive(roomId, "roomId");

        // 1. 실내 환경 분석 API를 호출한다.
        //    - 센서 기반 위험 여부, 상태 라벨, 조치 후보는 실내 환경 분석 결과를 우선한다.
        //    - recommendation은 환경 상태를 다시 판정하지 않는다.
        IndoorEnvironmentAnalysis analysis = fetchIndoorEnvironmentAnalysis(roomId);

        // 2. 스케줄러/내부 작업 전용 Core API로 강의실, 지역명, 기기 정보를 조회한다.
        //    - 이 흐름은 사용자 요청이 아니므로 userId, userRole을 받지 않는다.
        //    - teamId는 사용자 권한 검증이 아니라 팀별 브리핑/날씨 정책 조회 기준으로 사용한다.
        //    - 날씨 조회에는 건물 상세가 아니라 roomId 기준 regionName 조회 결과만 사용한다.
        RoomDetailResponse room = coreRoomService.getRoomDetailInternal(roomId);
        RoomRegionResponse roomRegion = coreRoomService.getRoomRegion(roomId);
        RoomDevicesResponse devices = coreRoomService.getRoomDevices(roomId);

        // 3. 팀별 외부 날씨 브리핑 정책을 조회한다.
        //    - 지금은 기본값을 사용하고, 이후 팀별 설정 DB/API가 생기면 이 메서드만 교체한다.
        WelcomeBriefingPolicyDto briefingPolicy = welcomeBriefingPolicyService.getPolicyOrDefault(teamId, roomId);

        // 4. 강의실 지역명 기준으로 외부 날씨와 오늘 예보를 조회한다.
        //    - 외부 날씨는 실내 환경 분석 결과의 조치 후보를 구체화하거나 주의점을 보완하는 데만 사용한다.
        //    - 예: 환기 필요 + 비/강풍 -> 창문 개방 대신 환기장치 또는 공기청정기 확인.
        KmaCurrentWeatherResponseDto currentWeather = weatherService.getCurrentWeather(roomRegion.regionName());
        KmaForecastWeatherResponseDto forecastWeather = weatherService.getForecastWeather(roomRegion.regionName());

        // 5. LLM 전달용 컨텍스트를 구성한다.
        //    - 실내 환경 분석 DTO는 원본 구조를 최대한 유지한다.
        //    - 날씨/예보/기기 목록은 조치 실행 가능성과 주의점 보강용 맥락이다.
        WelcomeBriefingContext context = new WelcomeBriefingContext(
                toRoomInfo(teamId, room, roomRegion, analysis),
                analysis,
                toCurrentWeatherSnapshot(currentWeather),
                toTodayWeatherOutlook(forecastWeather, briefingPolicy),
                toDeviceStatuses(devices)
        );

        // 6. LLM은 주어진 컨텍스트를 브리핑 문장으로 정리한다.
        //    - 환경 위험 판단과 조치 후보는 실내 환경 분석 결과를 우선한다.
        //    - 입력에 없는 수치나 상태는 생성하지 않도록 시스템 프롬프트에서 제한한다.
        return chatClient.prompt()
                .user(toJson(context))
                .call()
                .entity(WelcomeBriefingResponse.class);
    }

    private IndoorEnvironmentAnalysis fetchIndoorEnvironmentAnalysis(Long roomId) {
        // TODO: 실내 환경 분석 API 호출로 교체한다.
        // 예: indoorEnvironmentAnalysisClient.getWelcomeBriefingAnalysis(roomId)
        return new IndoorEnvironmentAnalysis(
                new IndoorEnvironmentAnalysis.TimeContext(
                        OffsetDateTime.of(2026, 8, 10, 8, 0, 0, 0, ZoneOffset.of("+09:00")),
                        OffsetDateTime.of(2026, 8, 10, 8, 0, 3, 0, ZoneOffset.of("+09:00")),
                        "Asia/Seoul",
                        "+09:00",
                        LocalDate.of(2026, 8, 10),
                        LocalTime.of(8, 0),
                        8,
                        DayOfWeek.MONDAY,
                        0,
                        0.1
                ),
                roomId,
                "실습실",
                10,
                new IndoorEnvironmentAnalysis.EnvironmentSummary(
                        25.0,
                        0.6,
                        42.0,
                        -2.0,
                        980.0,
                        1100.0,
                        250.0
                ),
                new IndoorEnvironmentAnalysis.SensorCoverage(
                        4,
                        4,
                        3,
                        1.0,
                        true,
                        Map.of(
                                "temperature", new IndoorEnvironmentAnalysis.MeasurementCoverage(4, 4, 1.0),
                                "humidity", new IndoorEnvironmentAnalysis.MeasurementCoverage(4, 4, 1.0),
                                "co2", new IndoorEnvironmentAnalysis.MeasurementCoverage(4, 4, 1.0)
                        )
                ),
                new IndoorEnvironmentAnalysis.CurrentState(
                        "DOMINANT",
                        "CO2_RISING_FAST",
                        "VENTILATE",
                        true,
                        true,
                        3,
                        3,
                        4,
                        4,
                        3,
                        1.0,
                        true,
                        Map.of("CO2_RISING_FAST", 3, "NO_ACTION", 1),
                        Map.of("VENTILATE", 3, "HOLD", 1),
                        List.of("VENTILATE")
                ),
                new IndoorEnvironmentAnalysis.LocationPreference(
                        23.5,
                        0.0,
                        "HIGH",
                        "SUPPORTED_ZERO_CROSSING",
                        61,
                        28,
                        5,
                        4.35,
                        0.465,
                        0.079,
                        null,
                        0.0,
                        "LOW",
                        "NO_SUPPORTED_ZERO_CROSSING",
                        77,
                        29,
                        13,
                        11.79,
                        0.702,
                        -0.009
                ),
                List.of("VENTILATION_INCREASE")
        );
    }

    private RoomInfo toRoomInfo(Long teamId,
                                RoomDetailResponse room,
                                RoomRegionResponse roomRegion,
                                IndoorEnvironmentAnalysis analysis) {
        return new RoomInfo(
                teamId,
                room.roomId(),
                room.roomName(),
                analysis.location(),
                roomRegion.regionName()
        );
    }

    private List<DeviceStatus> toDeviceStatuses(RoomDevicesResponse response) {
        if (response.devices() == null) {
            return List.of();
        }
        return response.devices().stream()
                .map(device -> DeviceStatus.normal(device.deviceId(), device.deviceName(), null))
                .toList();
    }

    private CurrentWeatherSnapshot toCurrentWeatherSnapshot(KmaCurrentWeatherResponseDto response) {
        return new CurrentWeatherSnapshot(
                response.regionName(),
                parseDateTime(response.baseDateTime()),
                response.temperature(),
                response.humidity(),
                response.precipitationType(),
                response.precipitationAmount(),
                response.windSpeed()
        );
    }

    private TodayWeatherOutlook toTodayWeatherOutlook(KmaForecastWeatherResponseDto response,
                                                      WelcomeBriefingPolicyDto policy) {
        List<KmaForecastWeatherResponseDto.Forecast> forecasts = response.forecasts();
        if (forecasts == null || forecasts.isEmpty()) {
            return new TodayWeatherOutlook(false, false, false, false, null, null, List.of("오늘 예보 데이터가 부족합니다."));
        }

        KmaForecastWeatherResponseDto.Forecast hottest = findHottestForecast(forecasts);
        WeatherOutlookFlags flags = analyzeWeatherOutlook(forecasts, policy);

        String hottestTime = hottest != null ? hottest.forecastDateTime() : null;
        String ventilationBestTime = (!flags.rainExpected() && !flags.strongWindExpected()) ? "비/강풍이 없는 시간대" : null;

        return new TodayWeatherOutlook(
                flags.rainExpected(),
                flags.rainPossible(),
                flags.strongWindExpected(),
                flags.highHumidityExpected(),
                hottestTime,
                ventilationBestTime,
                buildWeatherCautions(flags)
        );
    }

    private KmaForecastWeatherResponseDto.Forecast findHottestForecast(List<KmaForecastWeatherResponseDto.Forecast> forecasts) {
        return forecasts.stream()
                .max(Comparator.comparingDouble(forecast -> parseDouble(forecast.temperature())))
                .orElse(null);
    }

    private WeatherOutlookFlags analyzeWeatherOutlook(List<KmaForecastWeatherResponseDto.Forecast> forecasts,
                                                      WelcomeBriefingPolicyDto policy) {
        boolean rainExpected = false;
        boolean rainPossible = false;
        boolean strongWindExpected = false;
        boolean highHumidityExpected = false;

        for (KmaForecastWeatherResponseDto.Forecast forecast : forecasts) {
            rainExpected = rainExpected || isRainExpected(forecast, policy);
            rainPossible = rainPossible || isRainPossible(forecast, policy);
            strongWindExpected = strongWindExpected || isStrongWindExpected(forecast, policy);
            highHumidityExpected = highHumidityExpected || isHighHumidityExpected(forecast, policy);
        }

        return new WeatherOutlookFlags(rainExpected, rainPossible, strongWindExpected, highHumidityExpected);
    }

    private boolean isRainExpected(KmaForecastWeatherResponseDto.Forecast forecast,
                                   WelcomeBriefingPolicyDto policy) {
        return hasPrecipitation(forecast.precipitationType(), forecast.precipitationAmount())
                || parseInt(forecast.precipitationProbability()) >= policy.rainExpectedProbability();
    }

    private boolean isRainPossible(KmaForecastWeatherResponseDto.Forecast forecast,
                                   WelcomeBriefingPolicyDto policy) {
        int precipitationProbability = parseInt(forecast.precipitationProbability());
        return !isRainExpected(forecast, policy)
                && precipitationProbability >= policy.rainPossibleProbability();
    }

    private boolean isStrongWindExpected(KmaForecastWeatherResponseDto.Forecast forecast,
                                         WelcomeBriefingPolicyDto policy) {
        return parseDouble(forecast.windSpeed()) >= policy.strongWindSpeed();
    }

    private boolean isHighHumidityExpected(KmaForecastWeatherResponseDto.Forecast forecast,
                                           WelcomeBriefingPolicyDto policy) {
        return parseInt(forecast.humidity()) >= policy.highHumidityPercent();
    }

    private List<String> buildWeatherCautions(WeatherOutlookFlags flags) {
        List<String> cautions = new ArrayList<>();

        if (flags.rainExpected()) {
            cautions.add("비가 예상되어 창문 개방 환기는 주의가 필요합니다.");
        } else if (flags.rainPossible()) {
            cautions.add("비 가능성이 있어 창문 개방 전 외부 날씨 확인이 필요합니다.");
        }
        if (flags.strongWindExpected()) {
            cautions.add("강풍 가능성이 있어 창문 개방을 피하는 것이 좋습니다.");
        }
        if (flags.highHumidityExpected()) {
            cautions.add("외부 습도가 높을 수 있어 환기 후 실내 습도 확인이 필요합니다.");
        }

        return cautions;
    }

    private boolean hasPrecipitation(String precipitationType, String precipitationAmount) {
        boolean hasType = precipitationType != null
                && !precipitationType.isBlank()
                && !"없음".equals(precipitationType)
                && !"0".equals(precipitationType);
        boolean hasAmount = parseDouble(precipitationAmount) > 0.0;
        return hasType || hasAmount;
    }

    private record WeatherOutlookFlags(
            boolean rainExpected,
            boolean rainPossible,
            boolean strongWindExpected,
            boolean highHumidityExpected
    ) {
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next known KMA/core datetime format.
            }
        }
        return null;
    }

    private int parseInt(String value) {
        return (int) Math.round(parseDouble(value));
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        String normalized = value.replaceAll("[^0-9.+-]", "");
        if (normalized.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String toJson(WelcomeBriefingContext context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("웰컴 브리핑 컨텍스트 JSON 직렬화에 실패했습니다.", e);
        }
    }
}
