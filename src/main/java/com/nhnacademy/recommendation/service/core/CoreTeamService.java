package com.nhnacademy.recommendation.service.core;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.team.TeamResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.nhnacademy.recommendation.service.core.CoreRequestValidator.requireNonNull;
import static com.nhnacademy.recommendation.service.core.CoreRequestValidator.requirePositive;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoreTeamService {

    private final CoreClient coreClient;

    public List<TeamResponse> getTeamsByUser(Long userId, UserRole userRole) {
        requirePositive(userId, "userId");
        requireNonNull(userRole, "userRole");

        try {
            return coreClient.getTeamsByUser(userId, userRole).content();
        } catch (Exception e) {
            log.warn("[CoreTeamService] 팀 목록 조회 실패. userId={}, role={}", userId, userRole, e);
            throw e;
        }
    }
}
