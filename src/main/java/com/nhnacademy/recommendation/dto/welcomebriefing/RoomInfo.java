package com.nhnacademy.recommendation.dto.welcomebriefing;

public record RoomInfo(
        Long teamId,
        Long roomId,
        String roomName,
        String location,
        String regionName
) {
}
