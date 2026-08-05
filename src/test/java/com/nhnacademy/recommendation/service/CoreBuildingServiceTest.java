package com.nhnacademy.recommendation.service;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.dto.PageResponse;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.building.BuildingDetailResponse;
import com.nhnacademy.recommendation.dto.building.BuildingResponse;
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
class CoreBuildingServiceTest {

    @Mock
    CoreClient coreClient;

    CoreBuildingService service;

    @BeforeEach
    void setUp() {
        service = new CoreBuildingService(coreClient);
    }

    @Test
    @DisplayName("건물 목록 조회 성공")
    void getBuildingList() {
        List<BuildingResponse> buildings = List.of(new BuildingResponse(10L, 3L, "본관", "본관 설명"));
        given(coreClient.getBuildingListByTeam(1L, UserRole.NORMAL, 3L))
                .willReturn(new PageResponse<>(buildings, 0, 10, 1, 1, true, true));

        List<BuildingResponse> result = service.getBuildingList(1L, UserRole.NORMAL, 3L);

        assertThat(result).containsExactlyElementsOf(buildings);
    }

    @Test
    @DisplayName("건물 상세 조회 성공")
    void getBuildingDetail() {
        BuildingDetailResponse building = new BuildingDetailResponse(10L, 3L, "본관", "본관 설명", null, null, null, 0L, 0L, 0L);
        given(coreClient.getBuildingDetail(1L, UserRole.NORMAL, 3L, 10L)).willReturn(building);

        BuildingDetailResponse result = service.getBuildingDetail(1L, UserRole.NORMAL, 3L, 10L);

        assertThat(result).isEqualTo(building);
    }

    @Test
    @DisplayName("건물 목록 조회 실패 - 필수값 누락")
    void getBuildingList_Fail_RequiredValue() {
        assertThatThrownBy(() -> service.getBuildingList(1L, null, 3L))
                .isInstanceOf(RequiredValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("건물 상세 조회 실패 - 양수가 아닌 ID")
    void getBuildingDetail_Fail_NotPositiveValue() {
        assertThatThrownBy(() -> service.getBuildingDetail(1L, UserRole.NORMAL, 3L, 0L))
                .isInstanceOf(NotPositiveValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("건물 목록 조회 실패 - CoreClient 예외 전파")
    void getBuildingList_Fail_CoreClient() {
        RuntimeException exception = new RuntimeException("core api error");
        given(coreClient.getBuildingListByTeam(1L, UserRole.NORMAL, 3L)).willThrow(exception);

        assertThatThrownBy(() -> service.getBuildingList(1L, UserRole.NORMAL, 3L))
                .isSameAs(exception);
    }
}
