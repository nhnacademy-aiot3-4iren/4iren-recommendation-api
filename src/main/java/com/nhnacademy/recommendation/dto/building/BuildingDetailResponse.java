package com.nhnacademy.recommendation.dto.building;

public record BuildingDetailResponse(
        Long buildingId,
        Long teamId,
        String buildingName,
        String description,
        String roadAddress,
        String detailAddress,
        String regionName,
        long roomCount,
        long sensorCount,
        long deviceCount
) {
}
