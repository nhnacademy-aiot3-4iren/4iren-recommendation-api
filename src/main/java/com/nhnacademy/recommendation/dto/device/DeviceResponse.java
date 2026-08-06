package com.nhnacademy.recommendation.dto.device;

public record DeviceResponse(
        Long deviceId,
        Long roomId,
        String deviceName
) {

}
