package com.nhnacademy.recommendation.dto.welcomeBriefing;

public record RoomInfo(
        Long roomId,
        String roomName,
        Long buildingId,
        String buildingName,
        String regionName
) {
}
