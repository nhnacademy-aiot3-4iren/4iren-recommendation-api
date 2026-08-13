package com.nhnacademy.recommendation.service.core;


import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.building.BuildingDetailResponse;
import com.nhnacademy.recommendation.dto.building.BuildingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.nhnacademy.recommendation.service.core.CoreRequestValidator.requireNonNull;
import static com.nhnacademy.recommendation.service.core.CoreRequestValidator.requirePositive;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoreBuildingService {
    private final CoreClient coreClient;

    public BuildingDetailResponse getBuildingDetail(Long userId, UserRole userRole, Long teamId, Long buildingId) {
        requirePositive(userId, "userId");
        requirePositive(teamId, "teamId");
        requirePositive(buildingId, "buildingId");
        requireNonNull(userRole, "userRole");
        
        try{
            return coreClient.getBuildingDetail(userId, userRole, teamId, buildingId);
        }catch (Exception e){
            log.warn("[CoreBuildingService] 건물 상세 조회 실패. userId={}, role={}, teamId={}, buildingId={}",
                    userId, userRole, teamId, buildingId, e);
            throw e;
        }
    }

    public List<BuildingResponse> getBuildingList(Long userId, UserRole userRole, Long teamId) {
        requirePositive(userId, "userId");
        requirePositive(teamId, "teamId");
        requireNonNull(userRole, "userRole");

        try {
            return coreClient.getBuildingListByTeam(userId, userRole, teamId);
        } catch (Exception e) {
            log.warn("[CoreBuildingService] 건물 목록 조회 실패. userId={}, role={}, teamId={}",
                    userId, userRole, teamId, e);
            throw e;
        }
    }
}
