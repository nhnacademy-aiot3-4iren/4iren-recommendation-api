package com.nhnacademy.recommendation.dto.sensor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record SensorMetricSummaryResponse(
        Long roomId,
        Instant calculatedAt,
        Duration window,
        List<Metric> metrics
) {
    public record Metric(
            String metricCode,
            String displayName,
            String metricKind,
            String description,
            Double averageValue,
            String ucumCode,
            String unitDisplayName,
            String symbol
    ) {
    }
}
