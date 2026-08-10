package com.nhnacademy.recommendation.service.core;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.dto.PageResponse;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.room.RoomDetailResponse;
import com.nhnacademy.recommendation.dto.room.RoomDevicesResponse;
import com.nhnacademy.recommendation.dto.room.RoomRegionResponse;
import com.nhnacademy.recommendation.dto.room.RoomResponse;
import com.nhnacademy.recommendation.exception.NotPositiveValueException;
import com.nhnacademy.recommendation.exception.RequiredValueException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CoreRoomServiceTest {

    @Mock
    CoreClient coreClient;

    CoreRoomService service;

    @BeforeEach
    void setUp() {
        service = new CoreRoomService(coreClient);
    }

    @Test
    @DisplayName("건물 내 강의실 목록 조회 성공")
    void getRoomListByBuilding() {
        List<RoomResponse> rooms = List.of(new RoomResponse(20L, 10L, "101호", "강의실 설명"));
        given(coreClient.getRoomListByBuilding(1L, UserRole.NORMAL, 3L, 10L))
                .willReturn(new PageResponse<>(rooms, 0, 10, 1, 1, true, true));

        List<RoomResponse> result = service.getRoomListByBuilding(1L, UserRole.NORMAL, 3L, 10L);

        assertThat(result).containsExactlyElementsOf(rooms);
    }

    @Test
    @DisplayName("강의실 상세 조회 성공")
    void getRoomDetail() {
        RoomDetailResponse room = new RoomDetailResponse(20L, 10L, "본관", "101호", "강의실 설명", 0L, 0L);
        given(coreClient.getRoomDetail(1L, UserRole.NORMAL, 3L, 20L)).willReturn(room);

        RoomDetailResponse result = service.getRoomDetail(1L, UserRole.NORMAL, 3L, 20L);

        assertThat(result).isEqualTo(room);
    }

    @Test
    @DisplayName("Internal 강의실 상세 조회 성공")
    void getRoomDetailInternal() {
        RoomDetailResponse room = new RoomDetailResponse(20L, 10L, "본관", "101호", "강의실 설명", 0L, 0L);
        given(coreClient.getRoom(20L)).willReturn(room);

        RoomDetailResponse result = service.getRoomDetailInternal(20L);

        assertThat(result).isEqualTo(room);
    }

    @Test
    @DisplayName("Internal 강의실 지역명 조회 성공")
    void getRoomRegion() {
        RoomRegionResponse region = new RoomRegionResponse(20L, "광주");
        given(coreClient.getRoomRegion(20L)).willReturn(region);

        RoomRegionResponse result = service.getRoomRegion(20L);

        assertThat(result).isEqualTo(region);
    }

    @Test
    @DisplayName("Internal 강의실 기기 목록 조회 성공")
    void getRoomDevices() {
        RoomDevicesResponse devices = new RoomDevicesResponse(
                20L,
                "101호",
                List.of(new RoomDevicesResponse.DeviceSummary(1L, "환기장치"))
        );
        given(coreClient.getRoomDevices(20L)).willReturn(devices);

        RoomDevicesResponse result = service.getRoomDevices(20L);

        assertThat(result).isEqualTo(devices);
    }

    @Test
    @DisplayName("강의실 목록 조회 실패 - 필수값 누락")
    void getRoomListByBuilding_Fail_RequiredValue() {
        assertThatThrownBy(() -> service.getRoomListByBuilding(1L, null, 3L, 10L))
                .isInstanceOf(RequiredValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("강의실 상세 조회 실패 - 양수가 아닌 ID")
    void getRoomDetail_Fail_NotPositiveValue() {
        assertThatThrownBy(() -> service.getRoomDetail(1L, UserRole.NORMAL, 3L, -1L))
                .isInstanceOf(NotPositiveValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("Internal 강의실 상세 조회 실패 - 양수가 아닌 ID")
    void getRoomDetailInternal_Fail_NotPositiveValue() {
        assertThatThrownBy(() -> service.getRoomDetailInternal(0L))
                .isInstanceOf(NotPositiveValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("Internal 강의실 지역명 조회 실패 - 양수가 아닌 ID")
    void getRoomRegion_Fail_NotPositiveValue() {
        assertThatThrownBy(() -> service.getRoomRegion(0L))
                .isInstanceOf(NotPositiveValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("Internal 강의실 기기 목록 조회 실패 - 양수가 아닌 ID")
    void getRoomDevices_Fail_NotPositiveValue() {
        assertThatThrownBy(() -> service.getRoomDevices(0L))
                .isInstanceOf(NotPositiveValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("강의실 목록 조회 실패 - CoreClient 예외 전파")
    void getRoomListByBuilding_Fail_CoreClient() {
        RuntimeException exception = new RuntimeException("core api error");
        given(coreClient.getRoomListByBuilding(1L, UserRole.NORMAL, 3L, 10L)).willThrow(exception);

        assertThatThrownBy(() -> service.getRoomListByBuilding(1L, UserRole.NORMAL, 3L, 10L))
                .isSameAs(exception);
    }

    @Test
    @DisplayName("Internal 강의실 상세 조회 실패 - CoreClient 예외 전파")
    void getRoomDetailInternal_Fail_CoreClient() {
        RuntimeException exception = new RuntimeException("core api error");
        given(coreClient.getRoom(20L)).willThrow(exception);

        assertThatThrownBy(() -> service.getRoomDetailInternal(20L))
                .isSameAs(exception);
    }

    @Test
    @DisplayName("Internal 강의실 지역명 조회 실패 - CoreClient 예외 전파")
    void getRoomRegion_Fail_CoreClient() {
        RuntimeException exception = new RuntimeException("core api error");
        given(coreClient.getRoomRegion(20L)).willThrow(exception);

        assertThatThrownBy(() -> service.getRoomRegion(20L))
                .isSameAs(exception);
    }

    @Test
    @DisplayName("Internal 강의실 기기 목록 조회 실패 - CoreClient 예외 전파")
    void getRoomDevices_Fail_CoreClient() {
        RuntimeException exception = new RuntimeException("core api error");
        given(coreClient.getRoomDevices(20L)).willThrow(exception);

        assertThatThrownBy(() -> service.getRoomDevices(20L))
                .isSameAs(exception);
    }
}
