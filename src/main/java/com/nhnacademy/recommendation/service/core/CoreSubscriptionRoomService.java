package com.nhnacademy.recommendation.service.core;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.roomsub.RoomSubscriptionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.nhnacademy.recommendation.service.core.CoreRequestValidator.requireNonNull;
import static com.nhnacademy.recommendation.service.core.CoreRequestValidator.requirePositive;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoreSubscriptionRoomService {

    private final CoreClient coreClient;

    public List<RoomSubscriptionResponse> getSubscriptions(Long userId, UserRole userRole, Long teamId) {
        requirePositive(userId, "userId");
        requireNonNull(userRole, "userRole");
        requirePositive(teamId, "teamId");

        try {
            return coreClient.getSubscriptions(userId, userRole, teamId);
        } catch (Exception e) {
            log.warn("[CoreSubscriptionRoomService] 구독 강의실 목록 조회 실패. userId={}, role={}, teamId={}",
                    userId, userRole, teamId, e);
            throw e;
        }
    }

    public RoomSubscriptionResponse subscribeToRoom(Long userId, UserRole userRole, Long teamId, Long roomId) {
        requirePositive(userId, "userId");
        requireNonNull(userRole, "userRole");
        requirePositive(teamId, "teamId");
        requirePositive(roomId, "roomId");

        try {
            return coreClient.subscribeToRoom(userId, userRole, teamId, roomId);
        } catch (Exception e) {
            log.warn("[CoreSubscriptionRoomService] 구독 실패. userId={}, role={}, teamId={}, roomId={}",
                    userId, userRole, teamId, roomId, e);
            throw e;
        }

    }

    public void unsubscribeFromRoom(Long userId, UserRole userRole, Long teamId, Long roomId) {
        requirePositive(userId, "userId");
        requireNonNull(userRole, "userRole");
        requirePositive(teamId, "teamId");
        requirePositive(roomId, "roomId");

        try {
            coreClient.unsubscribeFromRoom(userId, userRole, teamId, roomId);
        } catch (Exception e) {
            log.warn("[CoreSubscriptionRoomService] 구독 취소 실패. userId={}, role={}, teamId={}, roomId={}",
                    userId, userRole, teamId, roomId, e);
            throw e;
        }
    }
}
