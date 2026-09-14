package com.nhnacademy.recommendation.service.core;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.roomsub.RoomSubscriptionResponse;
import com.nhnacademy.recommendation.exception.NotPositiveValueException;
import com.nhnacademy.recommendation.exception.RequiredValueException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CoreSubscriptionRoomServiceTest {

    @Mock
    CoreClient coreClient;

    CoreSubscriptionRoomService service;

    @BeforeEach
    void setUp() {
        service = new CoreSubscriptionRoomService(coreClient);
    }

    @Test
    @DisplayName("팀 내 구독 강의실 목록 조회 성공")
    void getSubscriptions() {
        List<RoomSubscriptionResponse> subscriptions = List.of(new RoomSubscriptionResponse(1L, 20L, true));
        given(coreClient.getSubscriptions(1L, UserRole.NORMAL, 3L))
                .willReturn(subscriptions);

        List<RoomSubscriptionResponse> result = service.getSubscriptions(1L, UserRole.NORMAL, 3L);

        assertThat(result).containsExactlyElementsOf(subscriptions);
    }

    @Test
    @DisplayName("팀 내 구독 강의실 목록 조회 실패 - 필수값 누락")
    void getSubscriptions_Fail_RequiredValue() {
        assertThatThrownBy(() -> service.getSubscriptions(1L, null, 3L))
                .isInstanceOf(RequiredValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("팀 내 구독 강의실 목록 조회 실패 - 양수가 아닌 ID")
    void getSubscriptions_Fail_NotPositiveValue() {
        assertThatThrownBy(() -> service.getSubscriptions(1L, UserRole.NORMAL, -1L))
                .isInstanceOf(NotPositiveValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("팀 내 구독 강의실 목록 조회 실패 - CoreClient 예외 전파")
    void getSubscriptions_Fail_CoreClient() {
        RuntimeException exception = new RuntimeException("core api error");
        given(coreClient.getSubscriptions(1L, UserRole.NORMAL, 3L)).willThrow(exception);

        assertThatThrownBy(() -> service.getSubscriptions(1L, UserRole.NORMAL, 3L))
                .isSameAs(exception);
    }

    @Test
    @DisplayName("강의실 구독 취소 성공")
    void unsubscribeFromRoom() {
        given(coreClient.unsubscribeFromRoom(1L, UserRole.NORMAL, 3L, 20L))
                .willReturn(ResponseEntity.noContent().build());

        service.unsubscribeFromRoom(1L, UserRole.NORMAL, 3L, 20L);

        verify(coreClient).unsubscribeFromRoom(1L, UserRole.NORMAL, 3L, 20L);
    }

    @Test
    @DisplayName("강의실 구독 취소 실패 - 필수값 누락")
    void unsubscribeFromRoom_Fail_RequiredValue() {
        assertThatThrownBy(() -> service.unsubscribeFromRoom(1L, null, 3L, 20L))
                .isInstanceOf(RequiredValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("강의실 구독 취소 실패 - 양수가 아닌 ID")
    void unsubscribeFromRoom_Fail_NotPositiveValue() {
        assertThatThrownBy(() -> service.unsubscribeFromRoom(1L, UserRole.NORMAL, 3L, 0L))
                .isInstanceOf(NotPositiveValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("강의실 구독 취소 실패 - CoreClient 예외 전파")
    void unsubscribeFromRoom_Fail_CoreClient() {
        RuntimeException exception = new RuntimeException("core api error");
        given(coreClient.unsubscribeFromRoom(1L, UserRole.NORMAL, 3L, 20L)).willThrow(exception);

        assertThatThrownBy(() -> service.unsubscribeFromRoom(1L, UserRole.NORMAL, 3L, 20L))
                .isSameAs(exception);
    }
}
