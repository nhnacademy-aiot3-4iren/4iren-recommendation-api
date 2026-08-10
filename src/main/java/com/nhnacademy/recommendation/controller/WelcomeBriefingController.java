package com.nhnacademy.recommendation.controller;

import com.nhnacademy.recommendation.dto.welcomeBriefing.WelcomeBriefingRequest;
import com.nhnacademy.recommendation.dto.welcomeBriefing.WelcomeBriefingResponse;
import com.nhnacademy.recommendation.service.core.CoreRequestValidator;
import com.nhnacademy.recommendation.service.welcomebriefing.WelcomeBriefingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/welcome-briefing")
public class WelcomeBriefingController {

    private final WelcomeBriefingService welcomeBriefingService;

    @PostMapping
    public WelcomeBriefingResponse generateWelcomeBriefing(@RequestBody(required = false) WelcomeBriefingRequest request) {
        CoreRequestValidator.requireNonNull(request, "WelcomeBriefingRequest");
        return welcomeBriefingService.generateWelcomeBriefing(request.teamId(), request.roomId());
    }
}
