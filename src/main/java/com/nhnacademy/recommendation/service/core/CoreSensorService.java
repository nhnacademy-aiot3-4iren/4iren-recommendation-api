package com.nhnacademy.recommendation.service.core;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.sensor.SensorLocationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
}
