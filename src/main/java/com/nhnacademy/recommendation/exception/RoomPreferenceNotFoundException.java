package com.nhnacademy.recommendation.exception;

public class RoomPreferenceNotFoundException extends ModelServingException {

    public RoomPreferenceNotFoundException(Long roomId) {
        super("모델 Bundle의 room_preference_profile.csv에 roomId가 없습니다. roomId:" + roomId);
    }
}
