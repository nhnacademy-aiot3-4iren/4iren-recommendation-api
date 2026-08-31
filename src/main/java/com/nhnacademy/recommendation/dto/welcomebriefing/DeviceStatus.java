package com.nhnacademy.recommendation.dto.welcomebriefing;

public record DeviceStatus(
        Long deviceId,
        String deviceName,
        String status
) {
    public static DeviceStatus normal(Long deviceId, String deviceName) {
        return new DeviceStatus(deviceId, deviceName, "정상 작동");
    }
}
