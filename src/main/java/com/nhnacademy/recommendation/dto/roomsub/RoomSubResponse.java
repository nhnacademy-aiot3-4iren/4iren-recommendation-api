package com.nhnacademy.recommendation.dto.roomsub;

public record RoomSubResponse(
        Long roomId,
        String roomName,
        boolean notificationEnabled
) {
}
