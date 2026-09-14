package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.llm.LlmConversationContext;
import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityType;
import com.nhnacademy.recommendation.dto.sensor.RoomSensorMetricSeriesResponse;
import com.nhnacademy.recommendation.dto.sensor.SensorMetricLatestResponse;
import com.nhnacademy.recommendation.dto.sensor.SensorMetricSummaryResponse;
import com.nhnacademy.recommendation.dto.tool.ToolResult;
import com.nhnacademy.recommendation.service.LlmConversationContextService;
import com.nhnacademy.recommendation.service.MentionedEntityResolver;
import com.nhnacademy.recommendation.service.core.CoreSensorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SensorMetricToolTest {

    @Mock CoreSensorService coreSensorService;
    @Mock LlmRequestContextHolder contextHolder;
    @Mock LlmConversationContextService conversationContextService;
    @Mock MentionedEntityResolver mentionedEntityResolver;

    SensorMetricTool tool;

    @BeforeEach
    void setUp() {
        tool = new SensorMetricTool(coreSensorService, contextHolder, conversationContextService,
                mentionedEntityResolver, new SensorToolAccessPolicy());
    }

    @Test
    @DisplayName("관리자는 강의실 현재 환경을 조회할 수 있다")
    void getCurrentRoomEnvironment() {
        SensorMetricSummaryResponse response = new SensorMetricSummaryResponse(
                20L, Instant.parse("2026-09-01T01:00:00Z"), Duration.ofMinutes(15), List.of());
        prepareContext(UserRole.ADMIN, 3L, 20L);
        given(coreSensorService.getSensorMetricSummary(1L, UserRole.ADMIN, 3L, 20L)).willReturn(response);

        ToolResult<SensorMetricSummaryResponse> result = tool.getCurrentRoomEnvironment(3L, 20L);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo(response);
        verify(conversationContextService).saveTeamMention(1L, 3L);
        verify(conversationContextService).saveRoomMention(1L, 20L, null);
    }

    @Test
    @DisplayName("소유자는 센서별 최신 측정값을 조회할 수 있다")
    void getLatestRoomSensorReadings() {
        SensorMetricLatestResponse response = new SensorMetricLatestResponse(
                20L, Instant.parse("2026-09-01T01:00:00Z"), Duration.ofHours(24), List.of());
        prepareContext(UserRole.OWNER, 3L, 20L);
        given(coreSensorService.getSensorMetricLatest(1L, UserRole.OWNER, 3L, 20L)).willReturn(response);

        ToolResult<SensorMetricLatestResponse> result = tool.getLatestRoomSensorReadings(3L, 20L);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo(response);
    }

    @Test
    @DisplayName("일반 사용자는 센서 데이터를 조회할 수 없다")
    void denyNormalUser() {
        prepareContext(UserRole.NORMAL, 3L, 20L);

        ToolResult<SensorMetricSummaryResponse> result = tool.getCurrentRoomEnvironment(3L, 20L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("ACCESS_DENIED_SENSOR_DATA");
        verifyNoInteractions(coreSensorService, conversationContextService);
    }

    @Test
    @DisplayName("대화에서 팀이나 강의실을 찾지 못하면 조회하지 않는다")
    void missingRoomCondition() {
        prepareContext(UserRole.ADMIN, null, 20L);

        ToolResult<SensorMetricSummaryResponse> result = tool.getCurrentRoomEnvironment(null, 20L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("MISSING_SENSOR_ROOM_CONDITION");
        verifyNoInteractions(coreSensorService, conversationContextService);
    }

    @Test
    @DisplayName("환경 변화 추이는 기본 3시간과 15분 간격으로 조회한다")
    void getRoomEnvironmentHistoryWithDefaults() {
        prepareContext(UserRole.ADMIN, 3L, 20L);
        given(coreSensorService.getRoomSensorMetricSeries(
                org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(UserRole.ADMIN),
                org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.eq(20L),
                org.mockito.ArgumentMatchers.eq("temperature"), any(Instant.class), any(Instant.class),
                org.mockito.ArgumentMatchers.eq(Duration.ofMinutes(15))))
                .willAnswer(invocation -> new RoomSensorMetricSeriesResponse(
                        20L, "temperature", "온도", "GAUGE", "실내 온도", "Cel", "섭씨", "°C",
                        invocation.getArgument(5), invocation.getArgument(6), invocation.getArgument(7), List.of()));

        ToolResult<RoomSensorMetricSeriesResponse> result =
                tool.getRoomEnvironmentHistory(3L, 20L, "temperature", null, null);

        assertThat(result.success()).isTrue();
        ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(coreSensorService).getRoomSensorMetricSeries(
                org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(UserRole.ADMIN),
                org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.eq(20L),
                org.mockito.ArgumentMatchers.eq("temperature"), fromCaptor.capture(), toCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(Duration.ofMinutes(15)));
        assertThat(Duration.between(fromCaptor.getValue(), toCaptor.getValue())).isEqualTo(Duration.ofHours(3));
        assertThat(toCaptor.getValue().toEpochMilli() % Duration.ofMinutes(15).toMillis()).isZero();
    }

    @Test
    @DisplayName("조회 기간이 집계 간격으로 나누어지지 않으면 실패한다")
    void invalidHistoryRange() {
        prepareContext(UserRole.ADMIN, 3L, 20L);

        ToolResult<RoomSensorMetricSeriesResponse> result =
                tool.getRoomEnvironmentHistory(3L, 20L, "temperature", 1, 17);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("INVALID_SENSOR_HISTORY_RANGE");
        verifyNoInteractions(coreSensorService, conversationContextService);
    }

    private void prepareContext(UserRole role, Long teamId, Long roomId) {
        given(contextHolder.get()).willReturn(new LlmRequestContext(1L, role, LlmConversationContext.empty()));
        given(mentionedEntityResolver.resolve(teamId, MentionedEntityType.TEAM)).willReturn(teamId);
        given(mentionedEntityResolver.resolve(roomId, MentionedEntityType.ROOM)).willReturn(roomId);
    }
}
