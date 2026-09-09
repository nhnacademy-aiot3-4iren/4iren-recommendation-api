package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityType;
import com.nhnacademy.recommendation.dto.sensor.RoomSensorMetricSeriesResponse;
import com.nhnacademy.recommendation.dto.sensor.SensorMetricLatestResponse;
import com.nhnacademy.recommendation.dto.sensor.SensorMetricSummaryResponse;
import com.nhnacademy.recommendation.dto.tool.ToolResult;
import com.nhnacademy.recommendation.service.LlmConversationContextService;
import com.nhnacademy.recommendation.service.MentionedEntityResolver;
import com.nhnacademy.recommendation.service.core.CoreSensorService;
import com.nhnacademy.recommendation.util.TimingLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class SensorMetricTool {

    private static final int DEFAULT_LOOKBACK_HOURS = 3;
    private static final int DEFAULT_INTERVAL_MINUTES = 15;
    private static final int MAX_LOOKBACK_HOURS = 24 * 7;
    private static final int MAX_INTERVAL_MINUTES = 24 * 60;

    private final CoreSensorService coreSensorService;
    private final LlmRequestContextHolder llmRequestContextHolder;
    private final LlmConversationContextService llmConversationContextService;
    private final MentionedEntityResolver mentionedEntityResolver;
    private final SensorToolAccessPolicy sensorToolAccessPolicy;

    @Tool(name = "get_current_room_environment", description = """
            강의실 전체 센서의 최근 15분 공간 평균을 조회해 현재 실내 환경을 확인합니다.
            현재 실내 온도, 습도, CO2, 공기 상태를 묻는 경우 이 도구를 호출하세요.
            """)
    public ToolResult<SensorMetricSummaryResponse> getCurrentRoomEnvironment(
            @ToolParam(required = false, description = "팀 번호. 질문에 없으면 생략하세요.") Long teamId,
            @ToolParam(required = false, description = "강의실 번호. 질문에 없으면 생략하세요.") Long roomId) {
        LlmRequestContext context = llmRequestContextHolder.get();
        ResolvedRoom resolved = resolveRoom(teamId, roomId);
        return TimingLog.measure(log,
                "[Timing][Tool] get_current_room_environment teamId=" + resolved.teamId() + " roomId=" + resolved.roomId(),
                () -> querySensorData(context, resolved, "현재 환경",
                        () -> coreSensorService.getSensorMetricSummary(
                                context.userId(), context.role(), resolved.teamId(), resolved.roomId())));
    }

    @Tool(name = "get_latest_room_sensor_readings", description = """
            강의실의 센서별 최신 측정값과 측정 시각을 조회합니다.
            각 센서, 센서별 차이, 특정 위치 센서의 최신 값을 묻는 경우 이 도구를 호출하세요.
            강의실의 대표적인 현재 환경만 필요하면 get_current_room_environment를 사용하세요.
            """)
    public ToolResult<SensorMetricLatestResponse> getLatestRoomSensorReadings(
            @ToolParam(required = false, description = "팀 번호. 질문에 없으면 생략하세요.") Long teamId,
            @ToolParam(required = false, description = "강의실 번호. 질문에 없으면 생략하세요.") Long roomId) {
        LlmRequestContext context = llmRequestContextHolder.get();
        ResolvedRoom resolved = resolveRoom(teamId, roomId);
        return TimingLog.measure(log,
                "[Timing][Tool] get_latest_room_sensor_readings teamId=" + resolved.teamId() + " roomId=" + resolved.roomId(),
                () -> querySensorData(context, resolved, "센서별 최신값",
                        () -> coreSensorService.getSensorMetricLatest(
                                context.userId(), context.role(), resolved.teamId(), resolved.roomId())));
    }

    @Tool(name = "get_room_environment_history", description = """
            특정 센서 메트릭의 강의실 공간 평균 변화 추이를 조회합니다.
            최근 몇 시간의 온도, 습도, CO2 추이 또는 언제부터 값이 변했는지 묻는 경우 호출하세요.
            lookbackHours 기본값은 3시간, intervalMinutes 기본값은 15분입니다.
            """)
    public ToolResult<RoomSensorMetricSeriesResponse> getRoomEnvironmentHistory(
            @ToolParam(required = false, description = "팀 번호. 질문에 없으면 생략하세요.") Long teamId,
            @ToolParam(required = false, description = "강의실 번호. 질문에 없으면 생략하세요.") Long roomId,
            @ToolParam(description = "조회할 메트릭 코드. 예: temperature, humidity, co2") String metricCode,
            @ToolParam(required = false, description = "현재부터 과거로 조회할 시간. 기본 3, 최대 168") Integer lookbackHours,
            @ToolParam(required = false, description = "평균 집계 간격(분). 기본 15") Integer intervalMinutes) {
        LlmRequestContext context = llmRequestContextHolder.get();
        ResolvedRoom resolved = resolveRoom(teamId, roomId);
        return TimingLog.measure(log,
                "[Timing][Tool] get_room_environment_history teamId=" + resolved.teamId() + " roomId=" + resolved.roomId(),
                () -> {
                    ToolResult<RoomSensorMetricSeriesResponse> accessFailure = validateAccessAndRoom(context, resolved);
                    if (accessFailure != null) {
                        return accessFailure;
                    }
                    if (metricCode == null || metricCode.isBlank()) {
                        return ToolResult.failure("MISSING_SENSOR_METRIC_CODE",
                                "조회할 센서 항목이 필요합니다. 온도, 습도, CO2 중 무엇을 조회할지 물어보세요.");
                    }

                    int resolvedLookbackHours = lookbackHours == null ? DEFAULT_LOOKBACK_HOURS : lookbackHours;
                    int resolvedIntervalMinutes = intervalMinutes == null ? DEFAULT_INTERVAL_MINUTES : intervalMinutes;
                    String invalidRangeMessage = validateHistoryRange(resolvedLookbackHours, resolvedIntervalMinutes);
                    if (invalidRangeMessage != null) {
                        return ToolResult.failure("INVALID_SENSOR_HISTORY_RANGE", invalidRangeMessage);
                    }

                    Duration interval = Duration.ofMinutes(resolvedIntervalMinutes);
                    Instant to = alignToInterval(Instant.now(), interval);
                    Instant from = to.minus(Duration.ofHours(resolvedLookbackHours));
                    try {
                        RoomSensorMetricSeriesResponse result = coreSensorService.getRoomSensorMetricSeries(
                                context.userId(), context.role(), resolved.teamId(), resolved.roomId(),
                                metricCode.trim(), from, to, interval);
                        saveRoomContext(context, resolved);
                        return ToolResult.success(result);
                    } catch (Exception e) {
                        log.warn("[SensorMetricTool] 강의실 환경 추이 조회 실패. teamId={}, roomId={}, metricCode={}",
                                resolved.teamId(), resolved.roomId(), metricCode, e);
                        return ToolResult.failure("ROOM_ENVIRONMENT_HISTORY_QUERY_FAILED",
                                "강의실 환경 변화 추이를 조회하지 못했습니다.");
                    }
                });
    }

    private <T> ToolResult<T> querySensorData(LlmRequestContext context, ResolvedRoom resolved,
                                               String queryName, SensorQuery<T> query) {
        ToolResult<T> accessFailure = validateAccessAndRoom(context, resolved);
        if (accessFailure != null) {
            return accessFailure;
        }
        try {
            T result = query.execute();
            saveRoomContext(context, resolved);
            return ToolResult.success(result);
        } catch (Exception e) {
            log.warn("[SensorMetricTool] {} 조회 실패. teamId={}, roomId={}",
                    queryName, resolved.teamId(), resolved.roomId(), e);
            return ToolResult.failure("SENSOR_DATA_QUERY_FAILED", "강의실 센서 데이터를 조회하지 못했습니다.");
        }
    }

    private <T> ToolResult<T> validateAccessAndRoom(LlmRequestContext context, ResolvedRoom resolved) {
        if (!sensorToolAccessPolicy.canRead(context.role())) {
            return ToolResult.failure("ACCESS_DENIED_SENSOR_DATA",
                    "센서 정보는 팀 관리자 또는 소유자만 조회할 수 있습니다.");
        }
        if (resolved.teamId() == null || resolved.roomId() == null) {
            return ToolResult.failure("MISSING_SENSOR_ROOM_CONDITION",
                    "팀 번호와 강의실 번호가 필요합니다. 어느 팀의 어떤 강의실인지 물어보세요.");
        }
        return null;
    }

    private String validateHistoryRange(int lookbackHours, int intervalMinutes) {
        if (lookbackHours < 1 || lookbackHours > MAX_LOOKBACK_HOURS) {
            return "조회 기간은 1시간 이상 168시간 이하로 지정해야 합니다.";
        }
        if (intervalMinutes < 1 || intervalMinutes > MAX_INTERVAL_MINUTES) {
            return "집계 간격은 1분 이상 1440분 이하로 지정해야 합니다.";
        }
        long rangeMinutes = Duration.ofHours(lookbackHours).toMinutes();
        if (intervalMinutes > rangeMinutes || rangeMinutes % intervalMinutes != 0) {
            return "조회 기간은 집계 간격으로 정확히 나누어져야 합니다.";
        }
        return null;
    }

    private Instant alignToInterval(Instant instant, Duration interval) {
        long intervalMillis = interval.toMillis();
        long alignedMillis = Math.floorDiv(instant.toEpochMilli(), intervalMillis) * intervalMillis;
        return Instant.ofEpochMilli(alignedMillis);
    }

    private ResolvedRoom resolveRoom(Long teamId, Long roomId) {
        return new ResolvedRoom(
                mentionedEntityResolver.resolve(teamId, MentionedEntityType.TEAM),
                mentionedEntityResolver.resolve(roomId, MentionedEntityType.ROOM));
    }

    private void saveRoomContext(LlmRequestContext context, ResolvedRoom resolved) {
        llmConversationContextService.saveTeamMention(context.userId(), resolved.teamId());
        llmConversationContextService.saveRoomMention(context.userId(), resolved.roomId(), null);
    }

    @FunctionalInterface
    private interface SensorQuery<T> {
        T execute();
    }

    private record ResolvedRoom(Long teamId, Long roomId) {
    }
}
