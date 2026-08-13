package com.nhnacademy.recommendation.dto.building;

public record BuildingResponse(
        Long buildingId,
        Long teamId,
        String buildingName,
        String description,
        String roadAddress,
        String detailAddress,
        String regionName
) {
}
