package com.nhnacademy.recommendation.service.welcomebriefing;

import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.team.TeamResponse;
import com.nhnacademy.recommendation.dto.team.TeamRole;
import com.nhnacademy.recommendation.dto.welcomeBriefing.WelcomeBriefingPolicyDto;
import com.nhnacademy.recommendation.entity.WelcomeBriefingPolicy;
import com.nhnacademy.recommendation.exception.InvalidPolicyRangeException;
import com.nhnacademy.recommendation.exception.NotPositiveValueException;
import com.nhnacademy.recommendation.exception.PolicyAccessDeniedException;
import com.nhnacademy.recommendation.exception.PolicyDuplicateException;
import com.nhnacademy.recommendation.exception.PolicyNotFoundException;
import com.nhnacademy.recommendation.exception.ProbabilityRangeException;
import com.nhnacademy.recommendation.exception.RequiredValueException;
import com.nhnacademy.recommendation.repository.WelcomeBriefingPolicyRepository;
import com.nhnacademy.recommendation.service.core.CoreTeamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.nhnacademy.recommendation.service.core.CoreRequestValidator.requireNonNull;
import static com.nhnacademy.recommendation.service.core.CoreRequestValidator.requirePositive;

@Service
@Slf4j
@RequiredArgsConstructor
public class WelcomeBriefingPolicyService {

    private final WelcomeBriefingPolicyRepository welcomeBriefingPolicyRepository;
    private final CoreTeamService coreTeamService;

    @Transactional(readOnly = true)
    public WelcomeBriefingPolicyDto getPolicyOrDefault(Long teamId, Long roomId) {
        validateScope(teamId, roomId);

        return welcomeBriefingPolicyRepository.findByTeamIdAndRoomIdAndEnabledTrue(teamId, roomId)
                .or(() -> welcomeBriefingPolicyRepository.findByTeamIdAndRoomIdAndEnabledTrue(teamId, null))
                .or(() -> welcomeBriefingPolicyRepository.findByTeamIdAndRoomIdAndEnabledTrue(null, null))
                .map(this::toDto)
                .orElseGet(WelcomeBriefingPolicyDto::defaultPolicy);
    }

    @Transactional
    public WelcomeBriefingPolicy createPolicy(Long userId,
                                              UserRole userRole,
                                              Long teamId,
                                              Long roomId,
                                              WelcomeBriefingPolicyDto policy) {
        validateWritablePolicyAccess(userId, userRole, teamId);
        validateScope(teamId, roomId);
        validatePolicy(policy);

        if (welcomeBriefingPolicyRepository.existsByTeamIdAndRoomId(teamId, roomId)) {
            throw new PolicyDuplicateException(teamId, roomId);
        }

        WelcomeBriefingPolicy entity = new WelcomeBriefingPolicy(
                teamId,
                roomId,
                policy.rainPossibleProbability(),
                policy.rainExpectedProbability(),
                policy.strongWindSpeed(),
                policy.highHumidityPercent(),
                policy.enabled()
        );

        return welcomeBriefingPolicyRepository.save(entity);
    }

    @Transactional
    public WelcomeBriefingPolicy updatePolicy(Long userId,
                                              UserRole userRole,
                                              Long teamId,
                                              Long roomId,
                                              WelcomeBriefingPolicyDto policy) {
        validateWritablePolicyAccess(userId, userRole, teamId);
        validateScope(teamId, roomId);
        validatePolicy(policy);

        WelcomeBriefingPolicy entity = findPolicyOrThrow(teamId, roomId);

        entity.updatePolicy(
                policy.rainPossibleProbability(),
                policy.rainExpectedProbability(),
                policy.strongWindSpeed(),
                policy.highHumidityPercent(),
                policy.enabled()
        );

        return entity;
    }

    @Transactional
    public void deletePolicy(Long userId, UserRole userRole, Long teamId, Long roomId) {
        validateWritablePolicyAccess(userId, userRole, teamId);
        validateScope(teamId, roomId);
        welcomeBriefingPolicyRepository.delete(findPolicyOrThrow(teamId, roomId));
    }

    @Transactional
    public void updatePolicyEnabled(Long userId,
                                    UserRole userRole,
                                    Long teamId,
                                    Long roomId,
                                    boolean enabled) {
        validateWritablePolicyAccess(userId, userRole, teamId);
        validateScope(teamId, roomId);
        findPolicyOrThrow(teamId, roomId).updateEnabled(enabled);
    }

    private WelcomeBriefingPolicy findPolicyOrThrow(Long teamId, Long roomId) {
        return welcomeBriefingPolicyRepository.findByTeamIdAndRoomId(teamId, roomId)
                .orElseThrow(() -> new PolicyNotFoundException(teamId, roomId));
    }

    private WelcomeBriefingPolicyDto toDto(WelcomeBriefingPolicy policy) {
        return new WelcomeBriefingPolicyDto(
                policy.getRainPossibleProbability(),
                policy.getRainExpectedProbability(),
                policy.getStrongWindSpeed(),
                policy.getHighHumidityPercent(),
                policy.getEnabled()
        );
    }

    private void validateScope(Long teamId, Long roomId) {
        if (teamId != null) {
            requirePositive(teamId, "teamId");
        }
        if (roomId != null) {
            requirePositive(roomId, "roomId");
        }
        if (teamId == null && roomId != null) {
            throw new RequiredValueException("teamId");
        }
    }

    private void validateWritablePolicyAccess(Long userId, UserRole userRole, Long teamId) {
        requirePositive(userId, "userId");
        requireNonNull(userRole, "userRole");

        if (teamId == null) {
            if (userRole != UserRole.ADMIN) {
                throw new PolicyAccessDeniedException(userId, null);
            }
            return;
        }

        List<TeamResponse> teams = coreTeamService.getTeamsByUser(userId, userRole);
        boolean writable = teams.stream()
                .filter(team -> team.teamId().equals(teamId))
                .map(TeamResponse::myRole)
                .anyMatch(role -> role == TeamRole.OWNER || role == TeamRole.ADMIN);

        if (!writable) {
            throw new PolicyAccessDeniedException(userId, teamId);
        }
    }

    private void validatePolicy(WelcomeBriefingPolicyDto policy) {
        if (policy == null) {
            throw new RequiredValueException("WelcomeBriefingPolicyDto");
        }

        validateProbability(policy.rainPossibleProbability(), "rainPossibleProbability");
        validateProbability(policy.rainExpectedProbability(), "rainExpectedProbability");

        if (policy.rainPossibleProbability() > policy.rainExpectedProbability()) {
            throw new InvalidPolicyRangeException(
                    "rainPossibleProbability",
                    policy.rainPossibleProbability(),
                    "rainExpectedProbability",
                    policy.rainExpectedProbability()
            );
        }
        if (policy.strongWindSpeed() < 0) {
            throw new NotPositiveValueException(policy.strongWindSpeed(),"강풍(StrongWindSpeed)");
        }
        validateProbability(policy.highHumidityPercent(), "highHumidityPercent");
    }

    private void validateProbability(int value, String fieldName) {
        if (value < 0 || value > 100) {
            throw new ProbabilityRangeException(value, fieldName);
        }
    }
}
