package com.nhnacademy.recommendation.dto.welcomeBriefing;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record IndoorEnvironmentAnalysis(
        TimeContext timeContext,
        Long roomId,
        String location,
        Integer measurementWindowMinutes,
        EnvironmentSummary environment,
        SensorCoverage sensorCoverage,
        CurrentState currentState,
        LocationPreference locationPreference,
        List<String> actionCandidates
) {
    public record TimeContext(
            OffsetDateTime observedAt,
            OffsetDateTime generatedAt,
            String timezone,
            String utcOffset,
            LocalDate localDate,
            LocalTime localTime,
            Integer hour,
            DayOfWeek weekday,
            Integer weekdayIndex,
            Double dataAgeMinutes
    ) {
    }

    public record EnvironmentSummary(
            Double temperatureC,
            Double temperatureChange30mC,
            Double humidityPercent,
            Double humidityChange30mPercentagePoint,
            Double co2MeanPpm,
            Double co2MaxPpm,
            Double co2Change30mMeanPpm
    ) {
    }

    public record SensorCoverage(
            Integer registeredSensorCount,
            Integer receivedSensorCount,
            Integer minimumReceivedRequired,
            Double coverageRatio,
            Boolean coverageSufficient,
            Map<String, MeasurementCoverage> byMeasurement
    ) {
    }

    public record MeasurementCoverage(
            Integer registeredSensorCount,
            Integer receivedSensorCount,
            Double coverageRatio
    ) {
    }

    public record CurrentState(
            String stateType,
            String primaryLabel,
            String primaryActionGroup,
            Boolean riskDetected,
            Boolean strictMajorityReached,
            Integer majorityCount,
            Integer majorityRequired,
            Integer registeredSensorCount,
            Integer receivedSensorCount,
            Integer minimumReceivedRequired,
            Double coverageRatio,
            Boolean coverageSufficient,
            Map<String, Integer> labelVotes,
            Map<String, Integer> actionGroupVotes,
            List<String> actionableGroups
    ) {
    }

    public record LocationPreference(
            Double preferredTemperatureC,
            Double temperatureOffsetC,
            String temperatureConfidence,
            String temperatureSupportStatus,
            Integer temperatureNeutralContextRows,
            Integer temperatureNeutralContextUniqueDays,
            Integer temperatureRecentFeedbackRows,
            Double temperatureEffectiveFeedbackRows,
            Double temperatureShrinkageFactor,
            Double temperatureAppliedResidualBias,
            Double preferredHumidityPercent,
            Double humidityOffsetPercentagePoint,
            String humidityConfidence,
            String humiditySupportStatus,
            Integer humidityNeutralContextRows,
            Integer humidityNeutralContextUniqueDays,
            Integer humidityRecentFeedbackRows,
            Double humidityEffectiveFeedbackRows,
            Double humidityShrinkageFactor,
            Double humidityAppliedResidualBias
    ) {
    }
}
