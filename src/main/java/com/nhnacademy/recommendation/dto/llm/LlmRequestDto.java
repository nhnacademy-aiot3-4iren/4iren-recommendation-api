package com.nhnacademy.recommendation.dto.llm;

import com.nhnacademy.recommendation.dto.roomsub.RoomSubResponse;

import java.time.LocalDateTime;
import java.util.List;

public record LlmRequestDto(

        List<RoomSubResponse> roomSubInfo,
        String message,
        LocalDateTime requestedAt
) {
}
