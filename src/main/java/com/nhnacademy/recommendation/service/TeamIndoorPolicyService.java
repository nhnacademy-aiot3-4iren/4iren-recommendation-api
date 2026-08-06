package com.nhnacademy.recommendation.service;

import com.nhnacademy.recommendation.dto.welcomeBriefing.IndoorEnvironmentPolicy;
import com.nhnacademy.recommendation.dto.welcomeBriefing.TeamIndoorPolicyOverride;
import com.nhnacademy.recommendation.dto.welcomeBriefing.TeamIndoorPolicyResponse;
import com.nhnacademy.recommendation.dto.welcomeBriefing.TeamIndoorPolicyUpdateRequest;
import com.nhnacademy.recommendation.entity.TeamIndoorPolicy;
import com.nhnacademy.recommendation.repository.TeamIndoorPolicyRepository;
import com.nhnacademy.recommendation.service.core.CoreRequestValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamIndoorPolicyService {

    private final TeamIndoorPolicyRepository repository;

    @Transactional(readOnly = true)
    public IndoorEnvironmentPolicy getEffectivePolicy(Long teamId) {
        CoreRequestValidator.requirePositive(teamId, "teamId");

        IndoorEnvironmentPolicy defaultPolicy = IndoorEnvironmentPolicy.defaults();
        return repository.findById(teamId)
                .map(TeamIndoorPolicy::toOverride)
                .map(override -> override.applyTo(defaultPolicy))
                .orElse(defaultPolicy);
    }

    @Transactional(readOnly = true)
    public TeamIndoorPolicyResponse getPolicy(Long teamId) {
        CoreRequestValidator.requirePositive(teamId, "teamId");

        IndoorEnvironmentPolicy defaultPolicy = IndoorEnvironmentPolicy.defaults();
        TeamIndoorPolicyOverride overridePolicy = repository.findById(teamId)
                .map(TeamIndoorPolicy::toOverride)
                .orElse(null);
        IndoorEnvironmentPolicy effectivePolicy = overridePolicy != null
                ? overridePolicy.applyTo(defaultPolicy)
                : defaultPolicy;

        return new TeamIndoorPolicyResponse(teamId, defaultPolicy, overridePolicy, effectivePolicy);
    }

    @Transactional
    public TeamIndoorPolicyResponse updatePolicy(Long teamId, TeamIndoorPolicyUpdateRequest request) {
        CoreRequestValidator.requirePositive(teamId, "teamId");
        CoreRequestValidator.requireNonNull(request, "request");

        TeamIndoorPolicy policy = repository.findById(teamId)
                .orElseGet(() -> new TeamIndoorPolicy(teamId));
        policy.update(request.toOverride(teamId));
        repository.save(policy);

        IndoorEnvironmentPolicy defaultPolicy = IndoorEnvironmentPolicy.defaults();
        TeamIndoorPolicyOverride overridePolicy = policy.toOverride();
        return new TeamIndoorPolicyResponse(teamId, defaultPolicy, overridePolicy, overridePolicy.applyTo(defaultPolicy));
    }
}
