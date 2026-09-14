package com.nhnacademy.recommendation.service.core;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.sensor.*;
import com.nhnacademy.recommendation.exception.RequiredValueException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static com.nhnacademy.recommendation.service.core.CoreRequestValidator.requireNonNull;
import static com.nhnacademy.recommendation.service.core.CoreRequestValidator.requirePositive;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoreSensorService {

    private final CoreClient coreClient;

    public List<SensorLocationResponse> getSensorListByRoom(Long userId, UserRole role, Long teamId, Long roomId) {
        requirePositive(userId, "userId");
        requireNonNull(role, "userRole");
        requirePositive(teamId, "teamId");
        requirePositive(roomId, "roomId");
        try {
            return coreClient.getSensorLocations(userId, role, teamId, roomId);
        } catch (Exception e) {
            log.warn("[CoreSensorService] 강의실 내 센서 목록 조회 실패. userId={}, role={}, teamId={}, roomId={}", userId, role, teamId, roomId, e);
            throw e;
        }
    }

    public SensorMetricCatalogResponse getSensorMetricCatalog(Long userId, UserRole role, Long teamId, Long roomId) {
        validatePublicSensorMetricRequest(userId, role, teamId, roomId);
        try {
            return coreClient.getSensorMetricCatalog(userId, role, teamId, roomId);
        } catch (Exception e) {
            log.warn("[CoreSensorService] 센서 메트릭 카탈로그 조회 실패. userId={}, role={}, teamId={}, roomId={}",
                    userId, role, teamId, roomId, e);
            throw e;
        }
    }

    public SensorMetricSummaryResponse getSensorMetricSummary(Long userId, UserRole role, Long teamId, Long roomId) {
        validatePublicSensorMetricRequest(userId, role, teamId, roomId);
        try {
            return coreClient.getSensorMetricSummary(userId, role, teamId, roomId);
        } catch (Exception e) {
            log.warn("[CoreSensorService] 센서 메트릭 최근 평균 조회 실패. userId={}, role={}, teamId={}, roomId={}",
                    userId, role, teamId, roomId, e);
            throw e;
        }
    }

    public SensorMetricLatestResponse getSensorMetricLatest(Long userId, UserRole role, Long teamId, Long roomId) {
        validatePublicSensorMetricRequest(userId, role, teamId, roomId);
        try {
            return coreClient.getSensorMetricLatest(userId, role, teamId, roomId);
        } catch (Exception e) {
            log.warn("[CoreSensorService] 센서별 최신 메트릭 조회 실패. userId={}, role={}, teamId={}, roomId={}",
                    userId, role, teamId, roomId, e);
            throw e;
        }
    }

    public RoomSensorMetricSeriesResponse getRoomSensorMetricSeries(
            Long userId,
            UserRole role,
            Long teamId,
            Long roomId,
            String metricCode,
            Instant from,
            Instant to,
            Duration interval
    ) {
        validatePublicSensorMetricRequest(userId, role, teamId, roomId);
        validateMetricCode(metricCode);
        validateSeriesRange(from, to, interval);

        try {
            return coreClient.getRoomSensorMetricSeries(userId, role, teamId, roomId, metricCode, from, to, interval);
        } catch (Exception e) {
            log.warn("[CoreSensorService] 공간 평균 센서 메트릭 시계열 조회 실패. userId={}, role={}, teamId={}, roomId={}, metricCode={}, from={}, to={}, interval={}",
                    userId, role, teamId, roomId, metricCode, from, to, interval, e);
            throw e;
        }
    }

    public SensorMetricSeriesResponse getSensorMetricSeries(
            Long userId,
            UserRole role,
            Long teamId,
            Long roomId,
            Instant from,
            Instant to,
            Duration interval
    ) {
        validatePublicSensorMetricRequest(userId, role, teamId, roomId);
        validateSeriesRange(from, to, interval);

        try {
            return coreClient.getSensorMetricSeries(userId, role, teamId, roomId, from, to, interval);
        } catch (Exception e) {
            log.warn("[CoreSensorService] 센서별 메트릭 시계열 조회 실패. userId={}, role={}, teamId={}, roomId={}, from={}, to={}, interval={}",
                    userId, role, teamId, roomId, from, to, interval, e);
            throw e;
        }
    }

    public SensorMetricSummaryResponse getSensorMetricSummaryInternal(Long roomId) {
        requirePositive(roomId, "roomId");
        try {
            return coreClient.getSensorMetricSummaryInternal(roomId);
        } catch (Exception e) {
            log.warn("[CoreSensorService] 센서 메트릭 최근 평균 조회 실패 - Internal. roomId={}", roomId, e);
            throw e;
        }
    }

    public SensorMetricSeriesResponse getSensorMetricSeriesInternal(
            Long roomId,
            Instant from,
            Instant to,
            Duration interval
    ) {
        requirePositive(roomId, "roomId");
        validateSeriesRange(from, to, interval);

        try {
            return coreClient.getSensorMetricSeriesInternal(roomId, from, to, interval);
        } catch (Exception e) {
            log.warn("[CoreSensorService] 센서별 메트릭 시계열 조회 실패 - Internal. roomId={}, from={}, to={}, interval={}",
                    roomId, from, to, interval, e);
            throw e;
        }
    }

    private void validatePublicSensorMetricRequest(Long userId, UserRole role, Long teamId, Long roomId) {
        requirePositive(userId, "userId");
        requireNonNull(role, "userRole");
        requirePositive(teamId, "teamId");
        requirePositive(roomId, "roomId");
    }

    private void validateMetricCode(String metricCode) {
        if (metricCode == null || metricCode.isBlank()) {
            throw new RequiredValueException("metricCode");
        }
    }

    private void validateSeriesRange(Instant from, Instant to, Duration interval) {
        requireNonNull(from, "from");
        requireNonNull(to, "to");
        requireNonNull(interval, "interval");

        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to.");
        }
        if (interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("interval must be positive.");
        }
        if (interval.toMillis() <= 0 || interval.toNanos() % 1_000_000 != 0) {
            throw new IllegalArgumentException("interval supports millisecond precision only.");
        }
        if (from.getNano() % 1_000_000 != 0 || to.getNano() % 1_000_000 != 0) {
            throw new IllegalArgumentException("from and to support millisecond precision only.");
        }
        if (Duration.between(from, to).toMillis() % interval.toMillis() != 0) {
            throw new IllegalArgumentException("range must be exactly divisible by interval.");
        }
    }
}
