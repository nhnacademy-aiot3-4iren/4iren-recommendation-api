package com.nhnacademy.recommendation.dto.sensor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record SensorMetricSeriesResponse(
        Long roomId,
        Instant from,
        Instant to,
        Duration interval,
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
            String ucumCode,
            String unitDisplayName,
            String symbol,
            List<Point> points
    ) {
    }

    public record Point(
            Instant bucketEndAt,
            Double averageValue
    ) {
    }
}
