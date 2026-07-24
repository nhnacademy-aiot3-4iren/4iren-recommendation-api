package com.nhnacademy.recommendation.dto.room;

public record RoomResponse(
        Long id,
        Long buildingId,
        String roomName
) {
}
