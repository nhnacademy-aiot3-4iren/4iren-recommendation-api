package com.nhnacademy.recommendation.service.core;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.sensor.*;
import com.nhnacademy.recommendation.exception.NotPositiveValueException;
import com.nhnacademy.recommendation.exception.RequiredValueException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CoreSensorServiceTest {

    @Mock
    CoreClient coreClient;

    CoreSensorService service;

    @BeforeEach
    void setUp() {
        service = new CoreSensorService(coreClient);
    }

    @Test
    @DisplayName("강의실 내 센서 목록 조회 성공")
    void getDeviceListByRoom() {
        List<SensorLocationResponse> sensors = List.of(new SensorLocationResponse(1L, 20L, "dev_EUI", "상세 위치"));
        given(coreClient.getSensorLocations(1L, UserRole.NORMAL, 3L, 20L))
                .willReturn(sensors);

        List<SensorLocationResponse> result = service.getSensorListByRoom(1L, UserRole.NORMAL, 3L, 20L);

        assertThat(result).containsExactlyElementsOf(sensors);
    }

    @Test
    @DisplayName("센서 메트릭 카탈로그 조회 성공")
    void getSensorMetricCatalog() {
        SensorMetricCatalogResponse response = new SensorMetricCatalogResponse(20L, List.of());
        given(coreClient.getSensorMetricCatalog(1L, UserRole.NORMAL, 3L, 20L)).willReturn(response);

        SensorMetricCatalogResponse result = service.getSensorMetricCatalog(1L, UserRole.NORMAL, 3L, 20L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("최근 15분 센서 메트릭 평균 조회 성공")
    void getSensorMetricSummary() {
        SensorMetricSummaryResponse response = new SensorMetricSummaryResponse(
                20L,
                Instant.parse("2026-08-04T02:00:00Z"),
                Duration.ofMinutes(15),
                List.of()
        );
        given(coreClient.getSensorMetricSummary(1L, UserRole.NORMAL, 3L, 20L)).willReturn(response);

        SensorMetricSummaryResponse result = service.getSensorMetricSummary(1L, UserRole.NORMAL, 3L, 20L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("센서별 최신 메트릭 조회 성공")
    void getSensorMetricLatest() {
        SensorMetricLatestResponse response = new SensorMetricLatestResponse(
                20L,
                Instant.parse("2026-08-04T02:00:00Z"),
                Duration.ofHours(24),
                List.of()
        );
        given(coreClient.getSensorMetricLatest(1L, UserRole.NORMAL, 3L, 20L)).willReturn(response);

        SensorMetricLatestResponse result = service.getSensorMetricLatest(1L, UserRole.NORMAL, 3L, 20L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("특정 메트릭 공간 평균 시계열 조회 성공")
    void getRoomSensorMetricSeries() {
        Instant from = Instant.parse("2026-08-04T00:00:00Z");
        Instant to = Instant.parse("2026-08-04T01:00:00Z");
        Duration interval = Duration.ofMinutes(15);
        RoomSensorMetricSeriesResponse response = new RoomSensorMetricSeriesResponse(
                20L,
                "temperature",
                "온도",
                "GAUGE",
                "실내 공기의 섭씨 온도",
                "Cel",
                "섭씨",
                "°C",
                from,
                to,
                interval,
                List.of()
        );
        given(coreClient.getRoomSensorMetricSeries(1L, UserRole.NORMAL, 3L, 20L, "temperature", from, to, interval))
                .willReturn(response);

        RoomSensorMetricSeriesResponse result = service.getRoomSensorMetricSeries(
                1L,
                UserRole.NORMAL,
                3L,
                20L,
                "temperature",
                from,
                to,
                interval
        );

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("센서별 메트릭 시계열 조회 성공")
    void getSensorMetricSeries() {
        Instant from = Instant.parse("2026-08-04T00:00:00Z");
        Instant to = Instant.parse("2026-08-04T01:00:00Z");
        Duration interval = Duration.ofMinutes(15);
        SensorMetricSeriesResponse response = new SensorMetricSeriesResponse(20L, from, to, interval, List.of());
        given(coreClient.getSensorMetricSeries(1L, UserRole.NORMAL, 3L, 20L, from, to, interval))
                .willReturn(response);

        SensorMetricSeriesResponse result = service.getSensorMetricSeries(
                1L,
                UserRole.NORMAL,
                3L,
                20L,
                from,
                to,
                interval
        );

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("내부용 최근 15분 센서 메트릭 평균 조회 성공")
    void getSensorMetricSummaryInternal() {
        SensorMetricSummaryResponse response = new SensorMetricSummaryResponse(
                20L,
                Instant.parse("2026-08-04T02:00:00Z"),
                Duration.ofMinutes(15),
                List.of()
        );
        given(coreClient.getSensorMetricSummaryInternal(20L)).willReturn(response);

        SensorMetricSummaryResponse result = service.getSensorMetricSummaryInternal(20L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("내부용 센서별 메트릭 시계열 조회 성공")
    void getSensorMetricSeriesInternal() {
        Instant from = Instant.parse("2026-08-04T00:00:00Z");
        Instant to = Instant.parse("2026-08-04T01:00:00Z");
        Duration interval = Duration.ofMinutes(15);
        SensorMetricSeriesResponse response = new SensorMetricSeriesResponse(20L, from, to, interval, List.of());
        given(coreClient.getSensorMetricSeriesInternal(20L, from, to, interval)).willReturn(response);

        SensorMetricSeriesResponse result = service.getSensorMetricSeriesInternal(20L, from, to, interval);

        assertThat(result).isEqualTo(response);
        verify(coreClient).getSensorMetricSeriesInternal(20L, from, to, interval);
    }

    @Test
    @DisplayName("강의실 내 센서 목록 조회 실패 - role 필수값 누락")
    void getDeviceListByRoom_Fail_RequiredValue() {
        assertThatThrownBy(() -> service.getSensorListByRoom(1L, null, 3L, 20L))
                .isInstanceOf(RequiredValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("강의실 내 센서 목록 조회 실패 - userId가 양수가 아님")
    void getDeviceListByRoom_Fail_InvalidUserId() {
        assertThatThrownBy(() -> service.getSensorListByRoom(0L, UserRole.NORMAL, 3L, 20L))
                .isInstanceOf(NotPositiveValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("강의실 내 센서 목록 조회 실패 - teamId가 양수가 아님")
    void getDeviceListByRoom_Fail_InvalidTeamId() {
        assertThatThrownBy(() -> service.getSensorListByRoom(1L, UserRole.NORMAL, 0L, 20L))
                .isInstanceOf(NotPositiveValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("강의실 내 센서 목록 조회 실패 - roomId가 양수가 아님")
    void getDeviceListByRoom_Fail_InvalidRoomId() {
        assertThatThrownBy(() -> service.getSensorListByRoom(1L, UserRole.NORMAL, 3L, 0L))
                .isInstanceOf(NotPositiveValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("강의실 내 센서 목록 조회 실패 - CoreClient 예외 전파")
    void getDeviceListByRoom_Fail_CoreClient() {
        RuntimeException exception = new RuntimeException("core api error");
        given(coreClient.getSensorLocations(1L, UserRole.NORMAL, 3L, 20L)).willThrow(exception);

        assertThatThrownBy(() -> service.getSensorListByRoom(1L, UserRole.NORMAL, 3L, 20L))
                .isSameAs(exception);
    }

    @Test
    @DisplayName("시계열 조회 실패 - 메트릭 코드 누락")
    void getRoomSensorMetricSeries_Fail_RequiredMetricCode() {
        assertThatThrownBy(() -> service.getRoomSensorMetricSeries(
                1L,
                UserRole.NORMAL,
                3L,
                20L,
                " ",
                Instant.parse("2026-08-04T00:00:00Z"),
                Instant.parse("2026-08-04T01:00:00Z"),
                Duration.ofMinutes(15)
        )).isInstanceOf(RequiredValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("시계열 조회 실패 - from이 to보다 늦거나 같음")
    void getSensorMetricSeries_Fail_InvalidRange() {
        assertThatThrownBy(() -> service.getSensorMetricSeries(
                1L,
                UserRole.NORMAL,
                3L,
                20L,
                Instant.parse("2026-08-04T01:00:00Z"),
                Instant.parse("2026-08-04T01:00:00Z"),
                Duration.ofMinutes(15)
        )).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("시계열 조회 실패 - 조회 기간이 interval로 나누어떨어지지 않음")
    void getSensorMetricSeries_Fail_NotDivisibleInterval() {
        assertThatThrownBy(() -> service.getSensorMetricSeriesInternal(
                20L,
                Instant.parse("2026-08-04T00:00:00Z"),
                Instant.parse("2026-08-04T01:10:00Z"),
                Duration.ofMinutes(15)
        )).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(coreClient);
    }
}
