package com.nhnacademy.recommendation.dto.roomsub;

import java.util.List;

public record RoomUnsubscriptionToolResponse(
        Long unsubscribedRoomId,
        List<RoomSubResponse> subscribedRooms
) {
    public static RoomUnsubscriptionToolResponse unsubscribed(Long roomId) {
        return new RoomUnsubscriptionToolResponse(roomId, List.of());
    }

    public static RoomUnsubscriptionToolResponse candidates(List<RoomSubResponse> subscribedRooms) {
        return new RoomUnsubscriptionToolResponse(null, subscribedRooms == null ? List.of() : subscribedRooms);
    }
}
