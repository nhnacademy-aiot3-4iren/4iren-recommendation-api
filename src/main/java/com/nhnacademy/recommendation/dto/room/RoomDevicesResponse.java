package com.nhnacademy.recommendation.dto.room;


import java.util.List;

public record RoomDevicesResponse(
        Long roomId,
        String roomName,
        List<DeviceSummary> devices
) {
    public record DeviceSummary(
            Long deviceId,
            String deviceName
    ) {
    }
}
