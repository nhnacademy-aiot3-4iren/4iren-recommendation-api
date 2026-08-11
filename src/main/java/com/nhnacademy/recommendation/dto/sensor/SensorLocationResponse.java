package com.nhnacademy.recommendation.dto.sensor;

public record SensorLocationResponse(
        Long sensorLocationId,
        Long roomId,
        String devEui,
        String locationDetail
) {
}
