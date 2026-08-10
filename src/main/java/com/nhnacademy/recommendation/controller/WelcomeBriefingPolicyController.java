package com.nhnacademy.recommendation.controller;

import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.welcomeBriefing.WelcomeBriefingPolicyDto;
import com.nhnacademy.recommendation.dto.welcomeBriefing.WelcomeBriefingPolicyEnabledRequest;
import com.nhnacademy.recommendation.dto.welcomeBriefing.WelcomeBriefingPolicyResponse;
import com.nhnacademy.recommendation.entity.WelcomeBriefingPolicy;
import com.nhnacademy.recommendation.service.welcomebriefing.WelcomeBriefingPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/welcome-briefing/policies")
public class WelcomeBriefingPolicyController {

    private final WelcomeBriefingPolicyService welcomeBriefingPolicyService;

    @GetMapping
    public WelcomeBriefingPolicyDto getPolicy(@RequestParam(required = false) Long teamId,
                                              @RequestParam(required = false) Long roomId) {
        return welcomeBriefingPolicyService.getPolicyOrDefault(teamId, roomId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WelcomeBriefingPolicyResponse createPolicy(@RequestHeader("X-USER-ID") Long userId,
                                                      @RequestHeader("X-USER-ROLE") UserRole userRole,
                                                      @RequestParam(required = false) Long teamId,
                                                      @RequestParam(required = false) Long roomId,
                                                      @RequestBody WelcomeBriefingPolicyDto request) {
        WelcomeBriefingPolicy policy = welcomeBriefingPolicyService.createPolicy(userId, userRole, teamId, roomId, request);
        return toResponse(policy);
    }

    @PutMapping
    public WelcomeBriefingPolicyResponse updatePolicy(@RequestHeader("X-USER-ID") Long userId,
                                                      @RequestHeader("X-USER-ROLE") UserRole userRole,
                                                      @RequestParam(required = false) Long teamId,
                                                      @RequestParam(required = false) Long roomId,
                                                      @RequestBody WelcomeBriefingPolicyDto request) {
        WelcomeBriefingPolicy policy = welcomeBriefingPolicyService.updatePolicy(userId, userRole, teamId, roomId, request);
        return toResponse(policy);
    }

    @PatchMapping("/enabled")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePolicyEnabled(@RequestHeader("X-USER-ID") Long userId,
                                    @RequestHeader("X-USER-ROLE") UserRole userRole,
                                    @RequestParam(required = false) Long teamId,
                                    @RequestParam(required = false) Long roomId,
                                    @RequestBody WelcomeBriefingPolicyEnabledRequest request) {
        welcomeBriefingPolicyService.updatePolicyEnabled(userId, userRole, teamId, roomId, request.enabled());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePolicy(@RequestHeader("X-USER-ID") Long userId,
                             @RequestHeader("X-USER-ROLE") UserRole userRole,
                             @RequestParam(required = false) Long teamId,
                             @RequestParam(required = false) Long roomId) {
        welcomeBriefingPolicyService.deletePolicy(userId, userRole, teamId, roomId);
    }

    private WelcomeBriefingPolicyResponse toResponse(WelcomeBriefingPolicy policy) {
        return new WelcomeBriefingPolicyResponse(
                policy.getId(),
                policy.getTeamId(),
                policy.getRoomId(),
                policy.getRainPossibleProbability(),
                policy.getRainExpectedProbability(),
                policy.getStrongWindSpeed(),
                policy.getHighHumidityPercent(),
                policy.getEnabled(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }
}
