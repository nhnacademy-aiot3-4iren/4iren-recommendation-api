package com.nhnacademy.recommendation.model.serving;

import java.util.Map;

public record RoomPreferenceProfile(
        Long roomId,
        String location,
        Map<String, String> values
) {

    public RoomPreferenceProfile {
        values = Map.copyOf(values);
    }
}
