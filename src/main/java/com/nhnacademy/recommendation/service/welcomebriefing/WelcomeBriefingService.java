package com.nhnacademy.recommendation.service.welcomebriefing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.dto.kma.KmaCurrentWeatherResponseDto;
import com.nhnacademy.recommendation.dto.kma.KmaForecastWeatherResponseDto;
import com.nhnacademy.recommendation.dto.room.RoomDetailResponse;
import com.nhnacademy.recommendation.dto.room.RoomDevicesResponse;
import com.nhnacademy.recommendation.dto.room.RoomRegionResponse;
import com.nhnacademy.recommendation.dto.sensor.SensorMetricSummaryResponse;
import com.nhnacademy.recommendation.dto.welcomebriefing.*;
import com.nhnacademy.recommendation.exception.ModelServingException;
import com.nhnacademy.recommendation.model.behavior.BehaviorRecommendation;
import com.nhnacademy.recommendation.service.behavior.BehaviorRecommendationService;
import com.nhnacademy.recommendation.service.core.CoreRequestValidator;
import com.nhnacademy.recommendation.service.core.CoreRoomService;
import com.nhnacademy.recommendation.service.core.CoreSensorService;
import com.nhnacademy.recommendation.service.core.CoreWeatherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class WelcomeBriefingService {

    private static final ZoneId BEHAVIOR_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final LocalTime WELCOME_BRIEFING_CUTOFF_TIME = LocalTime.of(10, 0);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final CoreWeatherService weatherService;
    private final CoreRoomService coreRoomService;
    private final CoreSensorService coreSensorService;
    private final WelcomeBriefingPolicyService welcomeBriefingPolicyService;
    private final ObjectProvider<BehaviorRecommendationService> behaviorRecommendationServiceProvider;
    private final Clock clock;

    @Autowired
    public WelcomeBriefingService(@Qualifier("welcomeBriefingChatClient") ChatClient chatClient,
                                  ObjectMapper objectMapper,
                                  CoreWeatherService weatherService,
                                  CoreRoomService coreRoomService,
                                  CoreSensorService coreSensorService,
                                  WelcomeBriefingPolicyService welcomeBriefingPolicyService,
                                  ObjectProvider<BehaviorRecommendationService> behaviorRecommendationServiceProvider) {
        this(chatClient, objectMapper, weatherService, coreRoomService, coreSensorService, welcomeBriefingPolicyService,
                behaviorRecommendationServiceProvider, Clock.system(BEHAVIOR_ZONE_ID));
    }

    WelcomeBriefingService(ChatClient chatClient,
                           ObjectMapper objectMapper,
                           CoreWeatherService weatherService,
                           CoreRoomService coreRoomService,
                           CoreSensorService coreSensorService,
                           WelcomeBriefingPolicyService welcomeBriefingPolicyService,
                           ObjectProvider<BehaviorRecommendationService> behaviorRecommendationServiceProvider,
                           Clock clock) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.weatherService = weatherService;
        this.coreRoomService = coreRoomService;
        this.coreSensorService = coreSensorService;
        this.welcomeBriefingPolicyService = welcomeBriefingPolicyService;
        this.behaviorRecommendationServiceProvider = behaviorRecommendationServiceProvider;
        this.clock = clock;
    }

    public WelcomeBriefingResponse generateWelcomeBriefing(Long teamId, Long roomId) {
        CoreRequestValidator.requirePositive(teamId, "teamId");
        CoreRequestValidator.requirePositive(roomId, "roomId");
        validateRequestTime();

        // 1. ML 추천 스케줄과 현재 센서 데이터를 조회한다.
        //    - ML 추천 스케줄은 이전 센서 데이터와 과거 기기 작동 이력을 반영한 오늘 관리 방안으로 사용한다.
        //    - 현재 센서 데이터는 조회 시점의 즉시 주의사항 판단에 사용한다.
        WelcomeBriefingMlRecommendation mlRecommendation = fetchMlRecommendation(roomId);
        CurrentSensorSnapshot currentSensor = fetchCurrentSensorSnapshot(roomId);

        // 2. 스케줄러/내부 작업 전용 Core API로 강의실, 지역명, 기기 정보를 조회한다.
        //    - 이 흐름은 사용자 요청이 아니므로 userId, userRole을 받지 않는다.
        //    - teamId는 사용자 권한 검증이 아니라 팀별 브리핑/날씨 정책 조회 기준으로 사용한다.
        //    - 날씨 조회에는 건물 상세가 아니라 roomId 기준 regionName 조회 결과만 사용한다.
        RoomDetailResponse room = coreRoomService.getRoomDetailInternal(roomId);
        RoomRegionResponse roomRegion = coreRoomService.getRoomRegion(roomId);
        RoomDevicesResponse devices = coreRoomService.getRoomDevices(roomId);

        // 3. 팀별 외부 날씨 브리핑 정책을 조회한다.
        WelcomeBriefingPolicyDto briefingPolicy = welcomeBriefingPolicyService.getPolicyOrDefault(teamId, roomId);

        // 4. 강의실 지역명 기준으로 외부 날씨와 오늘 예보를 조회한다.
        //    - 외부 날씨는 현재 센서 상태와 ML 추천 스케줄을 보정하거나 주의점을 보완하는 데만 사용한다.
        //    - 예: 환기 필요 + 비/강풍 -> 창문 개방 대신 환기장치 또는 공기청정기 확인.
        KmaCurrentWeatherResponseDto currentWeather = weatherService.getCurrentWeather(roomRegion.regionName());
        KmaForecastWeatherResponseDto forecastWeather = weatherService.getForecastWeather(roomRegion.regionName());

        // 5. LLM 전달용 컨텍스트를 구성한다.
        //    - 현재 센서 데이터와 ML 추천 스케줄은 원본 의미를 분리해서 전달한다.
        //    - 날씨/예보/기기 목록은 조치 실행 가능성과 주의점 보강용 맥락이다.
        WelcomeBriefingContext context = new WelcomeBriefingContext(
                toRoomInfo(teamId, room, roomRegion, mlRecommendation),
                currentSensor,
                toCurrentWeatherSnapshot(currentWeather),
                toTodayWeatherOutlook(forecastWeather, briefingPolicy),
                toDeviceStatuses(devices),
                mlRecommendation
        );

        // 6. LLM은 주어진 컨텍스트를 브리핑 문장으로 정리한다.
        //    - 현재 상태는 currentSensor를 우선하고, 하루 관리 방안은 mlRecommendation을 우선한다.
        //    - 입력에 없는 수치나 상태는 생성하지 않도록 시스템 프롬프트에서 제한한다.
        return chatClient.prompt()
                .user(toJson(context))
                .call()
                .entity(WelcomeBriefingResponse.class);
    }

    private void validateRequestTime() {
        if (!LocalTime.now(clock).isBefore(WELCOME_BRIEFING_CUTOFF_TIME)) {
            throw new IllegalArgumentException("웰컴 브리핑은 10시 전까지만 생성할 수 있습니다.");
        }
    }

    private CurrentSensorSnapshot fetchCurrentSensorSnapshot(Long roomId) {
        SensorMetricSummaryResponse summary = coreSensorService.getSensorMetricSummaryInternal(roomId);
        List<SensorMetricSummaryResponse.Metric> metrics = summary.metrics() == null
                ? List.of()
                : summary.metrics();
        Double temperature = findAverageValue(metrics, "temperature");
        Double humidity = findAverageValue(metrics, "humidity");
        Double co2 = findAverageValue(metrics, "co2");

        return new CurrentSensorSnapshot(
                summary.roomId() != null ? summary.roomId() : roomId,
                toServiceOffsetDateTime(summary.calculatedAt()),
                temperature,
                humidity,
                co2,
                null,
                null,
                temperature != null && humidity != null && co2 != null
        );
    }

    private Double findAverageValue(List<SensorMetricSummaryResponse.Metric> metrics, String metricCode) {
        return metrics.stream()
                .filter(metric -> metric.metricCode() != null)
                .filter(metric -> metric.metricCode().equalsIgnoreCase(metricCode))
                .map(SensorMetricSummaryResponse.Metric::averageValue)
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);
    }

    private OffsetDateTime toServiceOffsetDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return OffsetDateTime.ofInstant(instant, BEHAVIOR_ZONE_ID);
    }

    private WelcomeBriefingMlRecommendation fetchMlRecommendation(Long roomId) {
        LocalDate predictionDate = LocalDate.now(clock);
        BehaviorRecommendationService behaviorRecommendationService = behaviorRecommendationServiceProvider
                .getIfAvailable();
        if (behaviorRecommendationService == null) {
            throw new ModelServingException(
                    "Model serving이 비활성화되어 Behavior 추천을 생성할 수 없습니다."
            );
        }
        BehaviorRecommendation recommendation = behaviorRecommendationService.recommend(predictionDate, roomId);
        return toWelcomeBriefingMlRecommendation(recommendation);
    }

    private WelcomeBriefingMlRecommendation toWelcomeBriefingMlRecommendation(
            BehaviorRecommendation recommendation) {
        return new WelcomeBriefingMlRecommendation(
                recommendation.schemaVersion(),
                new WelcomeBriefingMlRecommendation.Context(
                        recommendation.context().predictionDate(),
                        recommendation.context().weekday(),
                        recommendation.context().roomId(),
                        recommendation.context().location(),
                        recommendation.context().timezone()
                ),
                recommendation.recommendationType(),
                recommendation.recommendedSchedule().stream()
                        .map(schedule -> new WelcomeBriefingMlRecommendation.RecommendedSchedule(
                                schedule.deviceType(),
                                schedule.action(),
                                schedule.startTime(),
                                schedule.endTime(),
                                schedule.confidence()
                        ))
                        .toList()
        );
    }

    private RoomInfo toRoomInfo(Long teamId,
                                RoomDetailResponse room,
                                RoomRegionResponse roomRegion,
                                WelcomeBriefingMlRecommendation mlRecommendation) {
        return new RoomInfo(
                teamId,
                room.roomId(),
                room.roomName(),
                mlRecommendation.context().location(),
                roomRegion.regionName()
        );
    }

    private List<DeviceStatus> toDeviceStatuses(RoomDevicesResponse response) {
        if (response.devices() == null) {
            return List.of();
        }
        return response.devices().stream()
                .map(device -> DeviceStatus.normal(device.deviceId(), device.deviceName()))
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
