package com.nhnacademy.recommendation.service.core;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.team.TeamResponse;
import com.nhnacademy.recommendation.dto.team.TeamRole;
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
class CoreTeamServiceTest {

    @Mock
    CoreClient coreClient;

    CoreTeamService service;

    @BeforeEach
    void setUp() {
        service = new CoreTeamService(coreClient);
    }

    @Test
    @DisplayName("사용자의 팀 목록 조회 성공")
    void getTeamsByUser() {
        List<TeamResponse> teams = List.of(new TeamResponse(3L, "3번팀", "3번팀 설명", TeamRole.NORMAL));
        given(coreClient.getTeamsByUser(1L, UserRole.NORMAL))
                .willReturn(teams);

        List<TeamResponse> result = service.getTeamsByUser(1L, UserRole.NORMAL);

        assertThat(result).containsExactlyElementsOf(teams);
    }

    @Test
    @DisplayName("사용자의 팀 목록 조회 실패 - 필수값 누락")
    void getTeamsByUser_Fail_RequiredValue() {
        assertThatThrownBy(() -> service.getTeamsByUser(1L, null))
                .isInstanceOf(RequiredValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("사용자의 팀 목록 조회 실패 - 양수가 아닌 ID")
    void getTeamsByUser_Fail_NotPositiveValue() {
        assertThatThrownBy(() -> service.getTeamsByUser(0L, UserRole.NORMAL))
                .isInstanceOf(NotPositiveValueException.class);

        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("사용자의 팀 목록 조회 실패 - CoreClient 예외 전파")
    void getTeamsByUser_Fail_CoreClient() {
        RuntimeException exception = new RuntimeException("core api error");
        given(coreClient.getTeamsByUser(1L, UserRole.NORMAL)).willThrow(exception);

        assertThatThrownBy(() -> service.getTeamsByUser(1L, UserRole.NORMAL))
                .isSameAs(exception);
    }
}
