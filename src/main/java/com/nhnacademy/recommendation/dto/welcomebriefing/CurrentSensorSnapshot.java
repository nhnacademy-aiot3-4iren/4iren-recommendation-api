package com.nhnacademy.recommendation.dto.welcomebriefing;

import java.time.OffsetDateTime;

public record CurrentSensorSnapshot(
        Long roomId,
        OffsetDateTime measuredAt,
        Double temperatureC,
        Double humidityPercent,
        Double co2Ppm,
        Integer registeredSensorCount,
        Integer receivedSensorCount,
        Boolean dataSufficient
) {
}
