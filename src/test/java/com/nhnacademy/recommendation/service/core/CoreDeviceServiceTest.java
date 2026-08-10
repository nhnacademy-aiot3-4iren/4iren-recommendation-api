package com.nhnacademy.recommendation.service.core;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.dto.PageResponse;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.device.DeviceResponse;
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
class CoreDeviceServiceTest {

    @Mock
    CoreClient coreClient;

    CoreDeviceService service;

    @BeforeEach
    void setUp() {
        service = new CoreDeviceService(coreClient);
    }

    @Test
    @DisplayName("강의실 내 기기 목록 조회 성공")
    void getDeviceListByRoom() {
        List<DeviceResponse> devices = List.of(new DeviceResponse(1L, 20L, "환기장치"));
        given(coreClient.getDevices(1L, UserRole.NORMAL, 3L, 20L))
                .willReturn(new PageResponse<>(devices, 0, 10, 1, 1, true, true));

        List<DeviceResponse> result = service.getDeviceListByRoom(1L, UserRole.NORMAL, 3L, 20L);

        assertThat(result).containsExactlyElementsOf(devices);
    }

    @Test
    @DisplayName("강의실 내 기기 목록 조회 실패 - role 필수값 누락")
    void getDeviceListByRoom_Fail_RequiredValue() {
        assertThatThrownBy(() -> service.getDeviceListByRoom(1L, null, 3L, 20L))
                .isInstanceOf(RequiredValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("강의실 내 기기 목록 조회 실패 - userId가 양수가 아님")
    void getDeviceListByRoom_Fail_InvalidUserId() {
        assertThatThrownBy(() -> service.getDeviceListByRoom(0L, UserRole.NORMAL, 3L, 20L))
                .isInstanceOf(NotPositiveValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("강의실 내 기기 목록 조회 실패 - teamId가 양수가 아님")
    void getDeviceListByRoom_Fail_InvalidTeamId() {
        assertThatThrownBy(() -> service.getDeviceListByRoom(1L, UserRole.NORMAL, 0L, 20L))
                .isInstanceOf(NotPositiveValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("강의실 내 기기 목록 조회 실패 - roomId가 양수가 아님")
    void getDeviceListByRoom_Fail_InvalidRoomId() {
        assertThatThrownBy(() -> service.getDeviceListByRoom(1L, UserRole.NORMAL, 3L, 0L))
                .isInstanceOf(NotPositiveValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("강의실 내 기기 목록 조회 실패 - CoreClient 예외 전파")
    void getDeviceListByRoom_Fail_CoreClient() {
        RuntimeException exception = new RuntimeException("core api error");
        given(coreClient.getDevices(1L, UserRole.NORMAL, 3L, 20L)).willThrow(exception);

        assertThatThrownBy(() -> service.getDeviceListByRoom(1L, UserRole.NORMAL, 3L, 20L))
                .isSameAs(exception);
    }
}
