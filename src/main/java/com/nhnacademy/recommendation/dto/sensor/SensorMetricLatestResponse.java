package com.nhnacademy.recommendation.dto.sensor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record SensorMetricLatestResponse(
        Long roomId,
        Instant queriedAt,
        Duration lookback,
        List<Sensor> sensors
) {
    public record Sensor(
            String devEui,
            List<Metric> metrics
    ) {
    }

    public record Metric(
            String metricCode,
            String displayName,
            String metricKind,
            String description,
            Double value,
            Instant measuredAt,
            String ucumCode,
            String unitDisplayName,
            String symbol
    ) {
    }
}
