package com.nhnacademy.recommendation.service.core;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.room.RoomDetailResponse;
import com.nhnacademy.recommendation.dto.room.RoomDevicesResponse;
import com.nhnacademy.recommendation.dto.room.RoomRegionResponse;
import com.nhnacademy.recommendation.dto.room.RoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.nhnacademy.recommendation.service.core.CoreRequestValidator.requireNonNull;
import static com.nhnacademy.recommendation.service.core.CoreRequestValidator.requirePositive;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoreRoomService {

    private final CoreClient coreClient;

    public List<RoomResponse> getRoomListByBuilding(Long userId, UserRole userRole, Long teamId, Long buildingId) {
        requirePositive(userId, "userId");
        requireNonNull(userRole, "userRole");
        requirePositive(teamId, "teamId");
        requirePositive(buildingId, "buildingId");

        try {
            return coreClient.getRoomListByBuilding(userId, userRole, teamId, buildingId).content();
        } catch (Exception e) {
            log.warn("[CoreRoomService] 강의실 목록 조회 실패. userId={}, role={}, teamId={}, buildingId={}",
                    userId, userRole, teamId, buildingId, e);
            throw e;
        }
    }

    public RoomDetailResponse getRoomDetail(Long userId, UserRole userRole, Long teamId, Long roomId) {
        requirePositive(userId, "userId");
        requireNonNull(userRole, "userRole");
        requirePositive(teamId, "teamId");
        requirePositive(roomId, "roomId");

        try {
            return coreClient.getRoomDetail(userId, userRole, teamId, roomId);
        } catch (Exception e) {
            log.warn("[CoreRoomService] 강의실 상세 조회 실패. userId={}, role={}, teamId={}, roomId={}",
                    userId, userRole, teamId, roomId, e);
            throw e;
        }
    }

    public RoomDetailResponse getRoomDetailInternal(Long roomId) {
        requirePositive(roomId, "roomId");
        try {
            return coreClient.getRoom(roomId);
        } catch (Exception e) {
            log.warn("[CoreRoomService] 강의실 상세 조회 실패 - Internal. roomId={}", roomId, e);
            throw e;
        }
    }

    public RoomRegionResponse getRoomRegion(Long roomId){
        requirePositive(roomId, "roomId");
        try {
            return coreClient.getRoomRegion(roomId);
        } catch (Exception e) {
            log.warn("[CoreRoomService] 강의실 지역명 조회 실패 - Internal. roomId={}", roomId, e);
            throw e;
        }
    }

    public RoomDevicesResponse getRoomDevices(Long roomId){
        requirePositive(roomId, "roomId");
        try {
            return coreClient.getRoomDevices(roomId);
        } catch (Exception e) {
            log.warn("[CoreRoomService] 강의실 지역명 조회 실패 - Internal. roomId={}", roomId, e);
            throw e;
        }
    }
}
