package com.nhnacademy.recommendation.dto.sensor;

import java.util.List;

public record SensorMetricCatalogResponse(
        Long roomId,
        List<Metric> metrics
) {
    public record Metric(
            String metricCode,
            String displayName,
            String metricKind,
            String description,
            String ucumCode,
            String unitDisplayName,
            String symbol,
            Integer supportedSensorCount,
            Boolean latestSupported,
            Boolean summarySupported,
            Boolean roomSeriesSupported,
            Boolean sensorSeriesSupported
    ) {
    }
}
