package com.nhnacademy.recommendation.adaptor;

import com.nhnacademy.recommendation.dto.PageResponse;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.building.BuildingDetailResponse;
import com.nhnacademy.recommendation.dto.building.BuildingResponse;
import com.nhnacademy.recommendation.dto.kma.KmaCurrentWeatherResponseDto;
import com.nhnacademy.recommendation.dto.kma.KmaForecastWeatherResponseDto;
import com.nhnacademy.recommendation.dto.room.RoomResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "4iren-core")
public interface CoreClient {

    @GetMapping("/api/kma/ultraSrtNcst")
    ResponseEntity<KmaCurrentWeatherResponseDto> getNcst(@RequestParam String regionName);

    @GetMapping("/api/kma/ultraSrtFcst")
    ResponseEntity<KmaForecastWeatherResponseDto> getFcst(@RequestParam String regionName);


    /// 팀에 속한 건물 리스트 조회
    @GetMapping("/api/teams/{teamId}/buildings")
    PageResponse<BuildingResponse> getBuildingListByTeam(@RequestHeader("X-User-Id") Long userId, @RequestHeader("X-User-Role") UserRole role, @PathVariable Long teamId);

    /// 건물 세부 정보 조회
    @GetMapping("/api/teams/{teamId}/buildings/{buildingId}")
    BuildingDetailResponse getBuildingDetail(@RequestHeader("X-User-Id") Long userId, @RequestHeader("X-User-Role") UserRole role, @PathVariable Long teamId, @PathVariable Long buildingId);

    /// 건물 내 강의실 리스트 조회
    @GetMapping("/api/teams/{teamId}/buildings/{buildingId}/rooms")
    PageResponse<RoomResponse> getRoomListByBuilding(@PathVariable Long teamId, @PathVariable Long buildingId);

    /// 강의실 세부 정보 조회
    @GetMapping("/api/teams/{teamId}/rooms/{roomId}")
    RoomResponse getRoomDetail(@PathVariable Long teamId, @PathVariable Long roomId);

}
