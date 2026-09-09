package com.nhnacademy.recommendation.service.dailysummary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.dto.dailysummary.DailySummaryContext;
import com.nhnacademy.recommendation.dto.dailysummary.DailySummaryResponse;
import com.nhnacademy.recommendation.dto.kma.KmaWeatherHistoryResponseDto;
import com.nhnacademy.recommendation.dto.room.RoomDetailResponse;
import com.nhnacademy.recommendation.dto.room.RoomRegionResponse;
import com.nhnacademy.recommendation.dto.sensor.SensorMetricSeriesResponse;
import com.nhnacademy.recommendation.service.DailyWeatherCacheService;
import com.nhnacademy.recommendation.service.core.CoreRequestValidator;
import com.nhnacademy.recommendation.service.core.CoreRoomService;
import com.nhnacademy.recommendation.service.core.CoreSensorService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.*;

@Service
public class DailySummaryService {

    public static final int DEFAULT_START_HOUR = 9;
    public static final int DEFAULT_END_HOUR = 18;
    private static final Duration SENSOR_INTERVAL = Duration.ofHours(1);
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalTime DAILY_SUMMARY_AVAILABLE_TIME = LocalTime.of(18, 20);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final CoreRoomService coreRoomService;
    private final CoreSensorService coreSensorService;
    private final DailyWeatherCacheService dailyWeatherCacheService;
    private final Clock clock;

    @Autowired
    public DailySummaryService(@Qualifier("dailySummaryChatClient") ChatClient chatClient,
                               ObjectMapper objectMapper,
                               CoreRoomService coreRoomService,
                               CoreSensorService coreSensorService,
                               DailyWeatherCacheService dailyWeatherCacheService) {
        this(chatClient, objectMapper, coreRoomService, coreSensorService, dailyWeatherCacheService,
                Clock.system(SERVICE_ZONE));
    }

    DailySummaryService(ChatClient chatClient,
                        ObjectMapper objectMapper,
                        CoreRoomService coreRoomService,
                        CoreSensorService coreSensorService,
                        DailyWeatherCacheService dailyWeatherCacheService,
                        Clock clock) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.coreRoomService = coreRoomService;
        this.coreSensorService = coreSensorService;
        this.dailyWeatherCacheService = dailyWeatherCacheService;
        this.clock = clock;
    }

    public DailySummaryResponse generateDailySummary(Long teamId,
                                                     Long roomId,
                                                     LocalDate date,
                                                     Integer startHour,
                                                     Integer endHour) {
        CoreRequestValidator.requirePositive(teamId, "teamId");
        CoreRequestValidator.requirePositive(roomId, "roomId");
        CoreRequestValidator.requireNonNull(date, "date");
        validateHourRange(startHour, endHour);
        validateRequestTime(date);

        RoomDetailResponse room = coreRoomService.getRoomDetailInternal(roomId);
        RoomRegionResponse region = coreRoomService.getRoomRegion(roomId);
        SensorMetricSeriesResponse indoorSensorSeries = coreSensorService.getSensorMetricSeriesInternal(
                roomId,
                toInstant(date, startHour),
                toInstant(date, endHour),
                SENSOR_INTERVAL
        );
        KmaWeatherHistoryResponseDto outdoorWeatherHistory = dailyWeatherCacheService.getDailyWeather(
                region.regionName(),
                date,
                startHour,
                endHour
        );

        DailySummaryContext context = new DailySummaryContext(
                teamId,
                roomId,
                date,
                new DailySummaryContext.AnalysisPeriod(
                        startHour,
                        endHour,
                        SERVICE_ZONE.getId(),
                        SENSOR_INTERVAL.toString()
                ),
                room,
                region,
                indoorSensorSeries,
                outdoorWeatherHistory
        );

        return chatClient.prompt()
                .user(toJson(context))
                .call()
                .entity(DailySummaryResponse.class);
    }

    private void validateHourRange(Integer startHour, Integer endHour) {
        CoreRequestValidator.requireNonNull(startHour, "startHour");
        CoreRequestValidator.requireNonNull(endHour, "endHour");
        if (startHour < 0 || startHour > 23 || endHour < 1 || endHour > 24) {
            throw new IllegalArgumentException("startHour는 0~23, endHour는 1~24 범위여야 합니다.");
        }
        if (startHour >= endHour) {
            throw new IllegalArgumentException("startHour는 endHour보다 작아야 합니다.");
        }
    }

    private void validateRequestTime(LocalDate date) {
        LocalDate today = LocalDate.now(clock);
        if (date.isAfter(today)) {
            throw new IllegalArgumentException("미래 날짜의 하루 요약은 생성할 수 없습니다.");
        }
        if (date.isEqual(today) && LocalTime.now(clock).isBefore(DAILY_SUMMARY_AVAILABLE_TIME)) {
            throw new IllegalArgumentException("오늘 하루 요약은 18시 20분 이후에 생성할 수 있습니다.");
        }
    }

    private Instant toInstant(LocalDate date, Integer hour) {
        return date.atTime(hour, 0).atZone(SERVICE_ZONE).toInstant();
    }

    private String toJson(DailySummaryContext context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("하루 요약 컨텍스트 JSON 직렬화에 실패했습니다.", e);
        }
    }
}
