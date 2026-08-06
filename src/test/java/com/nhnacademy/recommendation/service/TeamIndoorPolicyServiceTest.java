package com.nhnacademy.recommendation.service;

import com.nhnacademy.recommendation.dto.welcomeBriefing.IndoorEnvironmentPolicy;
import com.nhnacademy.recommendation.dto.welcomeBriefing.TeamIndoorPolicyResponse;
import com.nhnacademy.recommendation.dto.welcomeBriefing.TeamIndoorPolicyUpdateRequest;
import com.nhnacademy.recommendation.entity.TeamIndoorPolicy;
import com.nhnacademy.recommendation.exception.NotPositiveValueException;
import com.nhnacademy.recommendation.exception.RequiredValueException;
import com.nhnacademy.recommendation.repository.TeamIndoorPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TeamIndoorPolicyServiceTest {

    @Mock
    TeamIndoorPolicyRepository repository;

    TeamIndoorPolicyService service;

    @BeforeEach
    void setUp() {
        service = new TeamIndoorPolicyService(repository);
    }

    @Test
    @DisplayName("팀 정책이 없으면 기본 정책을 반환한다")
    void getEffectivePolicy_Default() {
        given(repository.findById(3L)).willReturn(Optional.empty());

        IndoorEnvironmentPolicy result = service.getEffectivePolicy(3L);

        assertThat(result).isEqualTo(IndoorEnvironmentPolicy.defaults());
    }

    @Test
    @DisplayName("팀 정책이 있으면 override와 기본 정책을 병합한다")
    void getEffectivePolicy_WithOverride() {
        TeamIndoorPolicy policy = new TeamIndoorPolicy(3L);
        policy.update(new TeamIndoorPolicyUpdateRequest(
                900,
                null,
                null,
                65.0,
                null,
                null,
                null,
                null,
                null,
                null,
                40,
                null
        ).toOverride(3L));
        given(repository.findById(3L)).willReturn(Optional.of(policy));

        IndoorEnvironmentPolicy result = service.getEffectivePolicy(3L);

        assertThat(result.co2WarningPpm()).isEqualTo(900);
        assertThat(result.humidityHighPercent()).isEqualTo(65.0);
        assertThat(result.rainPossibleProbability()).isEqualTo(40);
        assertThat(result.co2DangerPpm()).isEqualTo(IndoorEnvironmentPolicy.defaults().co2DangerPpm());
        assertThat(result.rainExpectedProbability()).isEqualTo(IndoorEnvironmentPolicy.defaults().rainExpectedProbability());
    }

    @Test
    @DisplayName("팀 정책 조회 시 기본 정책, override, 최종 정책을 함께 반환한다")
    void getPolicy() {
        TeamIndoorPolicy policy = new TeamIndoorPolicy(3L);
        policy.update(new TeamIndoorPolicyUpdateRequest(
                900,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                70
        ).toOverride(3L));
        given(repository.findById(3L)).willReturn(Optional.of(policy));

        TeamIndoorPolicyResponse result = service.getPolicy(3L);

        assertThat(result.teamId()).isEqualTo(3L);
        assertThat(result.defaultPolicy()).isEqualTo(IndoorEnvironmentPolicy.defaults());
        assertThat(result.overridePolicy()).isNotNull();
        assertThat(result.effectivePolicy().co2WarningPpm()).isEqualTo(900);
        assertThat(result.effectivePolicy().rainExpectedProbability()).isEqualTo(70);
    }

    @Test
    @DisplayName("팀 정책 수정 시 없으면 새로 만들고 저장한다")
    void updatePolicy_Create() {
        TeamIndoorPolicyUpdateRequest request = new TeamIndoorPolicyUpdateRequest(
                900,
                1400,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                40,
                70
        );
        given(repository.findById(3L)).willReturn(Optional.empty());

        TeamIndoorPolicyResponse result = service.updatePolicy(3L, request);

        ArgumentCaptor<TeamIndoorPolicy> captor = ArgumentCaptor.forClass(TeamIndoorPolicy.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTeamId()).isEqualTo(3L);
        assertThat(result.overridePolicy().co2WarningPpm()).isEqualTo(900);
        assertThat(result.effectivePolicy().co2DangerPpm()).isEqualTo(1400);
        assertThat(result.effectivePolicy().rainPossibleProbability()).isEqualTo(40);
        assertThat(result.effectivePolicy().rainExpectedProbability()).isEqualTo(70);
    }

    @Test
    @DisplayName("팀 정책 조회 실패 - 양수가 아닌 팀 ID")
    void getPolicy_Fail_NotPositiveTeamId() {
        assertThatThrownBy(() -> service.getPolicy(0L))
                .isInstanceOf(NotPositiveValueException.class);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("팀 정책 수정 실패 - 요청 DTO 누락")
    void updatePolicy_Fail_RequiredRequest() {
        assertThatThrownBy(() -> service.updatePolicy(3L, null))
                .isInstanceOf(RequiredValueException.class);

        verifyNoInteractions(repository);
    }
}
