package com.nhnacademy.recommendation.controller;

import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingRequest;
import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingResponse;
import com.nhnacademy.recommendation.service.core.CoreRequestValidator;
import com.nhnacademy.recommendation.service.welcomebriefing.WelcomeBriefingCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequiredArgsConstructor
@RequestMapping("/welcome-briefing")
public class WelcomeBriefingController {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final WelcomeBriefingCacheService welcomeBriefingCacheService;

    @PostMapping
    public WelcomeBriefingResponse generateWelcomeBriefing(@RequestBody(required = false) WelcomeBriefingRequest request) {
        CoreRequestValidator.requireNonNull(request, "WelcomeBriefingRequest");
        return welcomeBriefingCacheService.generateWelcomeBriefing(
                request.teamId(),
                request.roomId(),
                LocalDate.now(SERVICE_ZONE)
        );
    }

    @DeleteMapping("/cache")
    public ResponseEntity<Void> clearWelcomeBriefingCache() {
        welcomeBriefingCacheService.clearWelcomeBriefingCache();
        return ResponseEntity.noContent().build();
    }
}
