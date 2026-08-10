package com.nhnacademy.recommendation.dto.welcomeBriefing;

public record RoomInfo(
        Long teamId,
        Long roomId,
        String roomName,
        String location,
        String regionName
) {
}
