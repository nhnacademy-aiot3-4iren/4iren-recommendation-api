package com.nhnacademy.recommendation.adaptor;

import com.nhnacademy.recommendation.dto.PageResponse;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.building.BuildingDetailResponse;
import com.nhnacademy.recommendation.dto.building.BuildingResponse;
import com.nhnacademy.recommendation.dto.device.DeviceResponse;
import com.nhnacademy.recommendation.dto.kma.KmaCurrentWeatherResponseDto;
import com.nhnacademy.recommendation.dto.kma.KmaForecastWeatherResponseDto;
import com.nhnacademy.recommendation.dto.room.RoomDetailResponse;
import com.nhnacademy.recommendation.dto.room.RoomResponse;
import com.nhnacademy.recommendation.dto.roomsub.RoomSubscriptionResponse;
import com.nhnacademy.recommendation.dto.team.TeamResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "4iren-core", path = "/api/core")
public interface CoreClient {

    @GetMapping("/kma/ultraSrtNcst")
    ResponseEntity<KmaCurrentWeatherResponseDto> getNcst(@RequestParam("regionName") String regionName);

    @GetMapping("/kma/ultraSrtFcst")
    ResponseEntity<KmaForecastWeatherResponseDto> getFcst(@RequestParam("regionName") String regionName);


    /// 팀에 속한 건물 리스트 조회
    @GetMapping("/teams/{teamId}/buildings")
    PageResponse<BuildingResponse> getBuildingListByTeam(@RequestHeader("X-User-Id") Long userId, @RequestHeader("X-User-Role") UserRole role, @PathVariable Long teamId);

    /// 건물 세부 정보 조회
    @GetMapping("/teams/{teamId}/buildings/{buildingId}")
    BuildingDetailResponse getBuildingDetail(@RequestHeader("X-User-Id") Long userId, @RequestHeader("X-User-Role") UserRole role, @PathVariable Long teamId, @PathVariable Long buildingId);

    /// 건물 내 강의실 리스트 조회
    @GetMapping("/teams/{teamId}/buildings/{buildingId}/rooms")
    PageResponse<RoomResponse> getRoomListByBuilding(@RequestHeader("X-User-Id") Long userId, @RequestHeader("X-User-Role") UserRole role, @PathVariable Long teamId, @PathVariable Long buildingId);

    /// 강의실 세부 정보 조회
    @GetMapping("/teams/{teamId}/rooms/{roomId}")
    RoomDetailResponse getRoomDetail(@RequestHeader("X-User-Id") Long userId, @RequestHeader("X-User-Role") UserRole role, @PathVariable Long teamId, @PathVariable Long roomId);

    /// 사용자의 팀 목록 조회
    @GetMapping("/teams")
    PageResponse<TeamResponse> getTeamsByUser(@RequestHeader("X-User-Id") Long userId, @RequestHeader("X-User-Role") UserRole role);

    /// 사용자의 팀 내 구독중인 방 목록 조회
    @GetMapping("/teams/{teamId}/room-subscriptions")
    PageResponse<RoomSubscriptionResponse> getSubscriptions(@RequestHeader("X-User-Id") Long userId, @RequestHeader("X-User-Role") UserRole role, @PathVariable Long teamId);

    /// 강의실 내 기기 목록 조회
    @GetMapping("/teams/{teamId}/rooms/{roomId}/devices")
    PageResponse<DeviceResponse> getDevices(@RequestHeader("X-User-Id") Long userId, @RequestHeader("X-User-Role") UserRole role, @PathVariable Long teamId, @PathVariable Long roomId);

}
