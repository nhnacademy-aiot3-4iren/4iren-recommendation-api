package com.nhnacademy.recommendation.adaptor;

import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.building.BuildingDetailResponse;
import com.nhnacademy.recommendation.dto.building.BuildingResponse;
import com.nhnacademy.recommendation.dto.device.DeviceResponse;
import com.nhnacademy.recommendation.dto.kma.KmaCurrentWeatherResponseDto;
import com.nhnacademy.recommendation.dto.kma.KmaForecastWeatherResponseDto;
import com.nhnacademy.recommendation.dto.room.RoomDetailResponse;
import com.nhnacademy.recommendation.dto.room.RoomDevicesResponse;
import com.nhnacademy.recommendation.dto.room.RoomRegionResponse;
import com.nhnacademy.recommendation.dto.room.RoomResponse;
import com.nhnacademy.recommendation.dto.roomsub.RoomSubscriptionResponse;
import com.nhnacademy.recommendation.dto.sensor.SensorLocationResponse;
import com.nhnacademy.recommendation.dto.team.TeamResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "4iren-core", path = "/api/core")
public interface CoreClient {

    @GetMapping("/kma/ultraSrtNcst")
    ResponseEntity<KmaCurrentWeatherResponseDto> getNcst(@RequestParam("regionName") String regionName);

    @GetMapping("/kma/ultraSrtFcst")
    ResponseEntity<KmaForecastWeatherResponseDto> getFcst(@RequestParam("regionName") String regionName);

    @GetMapping("/internal/rooms/{room-id}")
    RoomDetailResponse getRoom(@PathVariable("room-id") Long roomId);

    @GetMapping("/internal/rooms/{room-id}/region")
    RoomRegionResponse getRoomRegion(@PathVariable("room-id") Long roomId);

    @GetMapping("/internal/rooms/{room-id}/devices")
    RoomDevicesResponse getRoomDevices(@PathVariable("room-id") Long roomId);


    /// 팀에 속한 건물 리스트 조회
    @GetMapping("/teams/{team-id}/buildings/all")
    List<BuildingResponse> getBuildingListByTeam(@RequestHeader("X-USER-ID") Long userId, @RequestHeader("X-USER-ROLE") UserRole role, @PathVariable("team-id") Long teamId);

    /// 건물 세부 정보 조회
    @GetMapping("/teams/{team-id}/buildings/{building-id}")
    BuildingDetailResponse getBuildingDetail(@RequestHeader("X-USER-ID") Long userId, @RequestHeader("X-USER-ROLE") UserRole role, @PathVariable("team-id") Long teamId, @PathVariable("building-id") Long buildingId);

    /// 건물 내 강의실 리스트 조회
    @GetMapping("/teams/{team-id}/buildings/{building-id}/rooms/all")
    List<RoomResponse> getRoomListByBuilding(@RequestHeader("X-USER-ID") Long userId, @RequestHeader("X-USER-ROLE") UserRole role, @PathVariable("team-id") Long teamId, @PathVariable("building-id") Long buildingId);

    /// 강의실 세부 정보 조회
    @GetMapping("/teams/{team-id}/rooms/{room-id}")
    RoomDetailResponse getRoomDetail(@RequestHeader("X-USER-ID") Long userId, @RequestHeader("X-USER-ROLE") UserRole role, @PathVariable("team-id") Long teamId, @PathVariable("room-id") Long roomId);

    /// 사용자의 팀 목록 조회
    @GetMapping("/teams/all")
    List<TeamResponse> getTeamsByUser(@RequestHeader("X-USER-ID") Long userId, @RequestHeader("X-USER-ROLE") UserRole role);

    /// 사용자의 팀 내 구독중인 방 목록 조회
    @GetMapping("/teams/{team-id}/room-subscriptions/all")
    List<RoomSubscriptionResponse> getSubscriptions(@RequestHeader("X-USER-ID") Long userId, @RequestHeader("X-USER-ROLE") UserRole role, @PathVariable("team-id") Long teamId);

    /// 강의실 내 기기 목록 조회
    @GetMapping("/teams/{team-id}/rooms/{room-id}/devices/all")
    List<DeviceResponse> getDevices(@RequestHeader("X-USER-ID") Long userId, @RequestHeader("X-USER-ROLE") UserRole role, @PathVariable("team-id") Long teamId, @PathVariable("room-id") Long roomId);

    /// 강의실 구독하기
    @PutMapping("/teams/{team-id}/rooms/{room-id}/subscription")
    RoomSubscriptionResponse subscribeToRoom(@RequestHeader("X-USER-ID") Long userId, @RequestHeader("X-USER-ROLE") UserRole role, @PathVariable("team-id") Long teamId, @PathVariable("room-id") Long roomId);

    /// 강의실 내 센서 목록 조회
    @GetMapping("/teams/{team-id}/rooms/{room-id}/sensor-locations/all")
    List<SensorLocationResponse> getSensorLocations(@RequestHeader("X-USER-ID") Long userId, @RequestHeader("X-USER-ROLE") UserRole role, @PathVariable("team-id") Long teamId, @PathVariable("room-id") Long roomId);
}
