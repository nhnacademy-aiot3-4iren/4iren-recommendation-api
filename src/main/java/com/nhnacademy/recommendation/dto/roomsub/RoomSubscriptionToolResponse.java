package com.nhnacademy.recommendation.dto.roomsub;

import com.nhnacademy.recommendation.dto.room.RoomResponse;

import java.util.List;

public record RoomSubscriptionToolResponse(
        RoomSubscriptionResponse subscription,
        List<RoomResponse> availableRooms
) {
    public static RoomSubscriptionToolResponse subscribed(RoomSubscriptionResponse subscription) {
        return new RoomSubscriptionToolResponse(subscription, List.of());
    }

    public static RoomSubscriptionToolResponse candidates(List<RoomResponse> availableRooms) {
        return new RoomSubscriptionToolResponse(null, availableRooms == null ? List.of() : availableRooms);
    }
}
