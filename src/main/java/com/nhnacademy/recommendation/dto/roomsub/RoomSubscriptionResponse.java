package com.nhnacademy.recommendation.dto.roomsub;


public record RoomSubscriptionResponse(
        Long roomSubscriptionId,
        Long roomId,
        boolean notificationEnabled
) {

}
