package com.nhnacademy.recommendation.dto.sensor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record RoomSensorMetricSeriesResponse(
        Long roomId,
        String metricCode,
        String displayName,
        String metricKind,
        String description,
        String ucumCode,
        String unitDisplayName,
        String symbol,
        Instant from,
        Instant to,
        Duration interval,
        List<Point> points
) {
    public record Point(
            Instant bucketEndAt,
            Double averageValue
    ) {
    }
}
