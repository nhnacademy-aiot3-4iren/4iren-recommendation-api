package com.nhnacademy.recommendation.model.behavior;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

public record BehaviorRecommendationResult(
        BehaviorRecommendation recommendation,
        String temperatureRegime,
        Map<String, DeviceUsageDecision> dailyUsage,
        Map<String, Object> eventSchedule
) {

    public BehaviorRecommendationResult {
        dailyUsage = Map.copyOf(dailyUsage);
        eventSchedule = Map.copyOf(eventSchedule);
    }

    public record DeviceUsageDecision(
            boolean useToday,
            double probability,
            double threshold,
            boolean allowedByRegime
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record HvacSchedule(
            String deviceType,
            boolean useToday,
            double usageProbability,
            double usageThreshold,
            int recommendedSessions,
            SessionProfile sessionProfile,
            List<Session> sessions
    ) {

        public HvacSchedule {
            sessions = sessions == null ? null : List.copyOf(sessions);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionProfile(
            int availableDays,
            int activeDays,
            double meanEventsOnActiveDays,
            int recommendedSessions,
            boolean usedSameWeekday,
            String reason
    ) {
    }

    public record Session(
            String startTime,
            double startProbability,
            String stopTime,
            double stopProbability
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record VentilationSchedule(
            int recommendedEvents,
            Double meanEventsPerDay,
            Boolean usedSameWeekday,
            List<VentilationEvent> events
    ) {

        public VentilationSchedule {
            events = events == null ? null : List.copyOf(events);
        }
    }

    public record VentilationEvent(String startTime, double probability) {
    }
}
