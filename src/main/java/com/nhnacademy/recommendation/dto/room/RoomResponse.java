package com.nhnacademy.recommendation.dto.room;

public record RoomResponse(
        Long roomId,
        Long buildingId,
        String roomName,
        String description
) {
}
