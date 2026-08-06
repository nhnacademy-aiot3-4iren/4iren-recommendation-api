package com.nhnacademy.recommendation.dto.welcomeBriefing;

public record DeviceStatus(
        Long deviceId,
        String deviceName,
        String deviceType,
        String status
) {
    public static DeviceStatus normal(Long deviceId, String deviceName, String deviceType) {
        return new DeviceStatus(deviceId, deviceName, deviceType, "정상 작동");
    }
}
