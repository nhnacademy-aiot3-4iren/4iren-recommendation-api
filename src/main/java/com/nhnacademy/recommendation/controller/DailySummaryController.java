package com.nhnacademy.recommendation.controller;

import com.nhnacademy.recommendation.dto.dailysummary.DailySummaryRequest;
import com.nhnacademy.recommendation.dto.dailysummary.DailySummaryResponse;
import com.nhnacademy.recommendation.service.core.CoreRequestValidator;
import com.nhnacademy.recommendation.service.dailysummary.DailySummaryCacheService;
import com.nhnacademy.recommendation.service.dailysummary.DailySummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequiredArgsConstructor
@RequestMapping("/daily-summary")
public class DailySummaryController {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final DailySummaryCacheService dailySummaryCacheService;

    @PostMapping
    public DailySummaryResponse generateDailySummary(@RequestBody(required = false) DailySummaryRequest request) {
        CoreRequestValidator.requireNonNull(request, "DailySummaryRequest");
        LocalDate date = request.date() != null ? request.date() : LocalDate.now(SERVICE_ZONE);
        Integer startHour = request.startHour() != null ? request.startHour() : DailySummaryService.DEFAULT_START_HOUR;
        Integer endHour = request.endHour() != null ? request.endHour() : DailySummaryService.DEFAULT_END_HOUR;

        return dailySummaryCacheService.generateDailySummary(
                request.teamId(),
                request.roomId(),
                date,
                startHour,
                endHour
        );
    }

    @DeleteMapping("/cache")
    public ResponseEntity<Void> clearDailySummaryCache() {
        dailySummaryCacheService.clearDailySummaryCache();
        return ResponseEntity.noContent().build();
    }
}
