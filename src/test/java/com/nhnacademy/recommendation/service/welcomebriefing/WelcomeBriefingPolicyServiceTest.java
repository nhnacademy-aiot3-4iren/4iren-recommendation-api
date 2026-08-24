package com.nhnacademy.recommendation.service.welcomebriefing;

import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.team.TeamResponse;
import com.nhnacademy.recommendation.dto.team.TeamRole;
import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingPolicyDto;
import com.nhnacademy.recommendation.entity.WelcomeBriefingPolicy;
import com.nhnacademy.recommendation.exception.InvalidPolicyRangeException;
import com.nhnacademy.recommendation.exception.PolicyAccessDeniedException;
import com.nhnacademy.recommendation.exception.PolicyDuplicateException;
import com.nhnacademy.recommendation.exception.ProbabilityRangeException;
import com.nhnacademy.recommendation.repository.WelcomeBriefingPolicyRepository;
import com.nhnacademy.recommendation.service.core.CoreTeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class WelcomeBriefingPolicyServiceTest {

    @Mock
    WelcomeBriefingPolicyRepository repository;

    @Mock
    CoreTeamService coreTeamService;

    WelcomeBriefingPolicyService service;

    @BeforeEach
    void setUp() {
        service = new WelcomeBriefingPolicyService(repository, coreTeamService);
    }

    @Test
    @DisplayName("강의실 정책이 있으면 강의실 정책을 우선 반환한다")
    void getPolicyOrDefault_RoomPolicy() {
        WelcomeBriefingPolicy roomPolicy = new WelcomeBriefingPolicy(3L, 10L, 20, 50, 6.0, 65, true);
        given(repository.findByTeamIdAndRoomIdAndEnabledTrue(3L, 10L)).willReturn(Optional.of(roomPolicy));

        WelcomeBriefingPolicyDto result = service.getPolicyOrDefault(3L, 10L);

        assertThat(result).isEqualTo(new WelcomeBriefingPolicyDto(20, 50, 6.0, 65, true));
    }

    @Test
    @DisplayName("강의실 정책이 없으면 팀 정책을 반환한다")
    void getPolicyOrDefault_TeamPolicy() {
        WelcomeBriefingPolicy teamPolicy = new WelcomeBriefingPolicy(3L, null, 25, 55, 7.0, 68, true);
        given(repository.findByTeamIdAndRoomIdAndEnabledTrue(3L, 10L)).willReturn(Optional.empty());
        given(repository.findByTeamIdAndRoomIdAndEnabledTrue(3L, null)).willReturn(Optional.of(teamPolicy));

        WelcomeBriefingPolicyDto result = service.getPolicyOrDefault(3L, 10L);

        assertThat(result).isEqualTo(new WelcomeBriefingPolicyDto(25, 55, 7.0, 68, true));
    }

    @Test
    @DisplayName("저장된 정책이 없으면 기본 정책을 반환한다")
    void getPolicyOrDefault_DefaultPolicy() {
        given(repository.findByTeamIdAndRoomIdAndEnabledTrue(3L, 10L)).willReturn(Optional.empty());
        given(repository.findByTeamIdAndRoomIdAndEnabledTrue(3L, null)).willReturn(Optional.empty());
        given(repository.findByTeamIdAndRoomIdAndEnabledTrue(null, null)).willReturn(Optional.empty());

        WelcomeBriefingPolicyDto result = service.getPolicyOrDefault(3L, 10L);

        assertThat(result).isEqualTo(WelcomeBriefingPolicyDto.defaultPolicy());
    }

    @Test
    @DisplayName("팀 관리자 권한이 있으면 정책을 생성한다")
    void createPolicy() {
        WelcomeBriefingPolicyDto request = new WelcomeBriefingPolicyDto(30, 60, 8.0, 70, true);
        given(coreTeamService.getTeamsByUser(1L, UserRole.NORMAL))
                .willReturn(List.of(new TeamResponse(3L, "3번팀", "설명", TeamRole.ADMIN)));
        given(repository.existsByTeamIdAndRoomId(3L, 10L)).willReturn(false);
        given(repository.save(any(WelcomeBriefingPolicy.class))).willAnswer(invocation -> invocation.getArgument(0));

        WelcomeBriefingPolicy result = service.createPolicy(1L, UserRole.NORMAL, 3L, 10L, request);

        assertThat(result.getTeamId()).isEqualTo(3L);
        assertThat(result.getRoomId()).isEqualTo(10L);
        assertThat(result.getRainPossibleProbability()).isEqualTo(30);
        assertThat(result.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("팀 관리 권한이 없으면 정책을 생성하지 않는다")
    void createPolicy_AccessDenied() {
        WelcomeBriefingPolicyDto request = new WelcomeBriefingPolicyDto(30, 60, 8.0, 70, true);
        given(coreTeamService.getTeamsByUser(1L, UserRole.NORMAL))
                .willReturn(List.of(new TeamResponse(3L, "3번팀", "설명", TeamRole.NORMAL)));

        assertThatThrownBy(() -> service.createPolicy(1L, UserRole.NORMAL, 3L, 10L, request))
                .isInstanceOf(PolicyAccessDeniedException.class);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("이미 같은 scope 정책이 있으면 중복 예외를 던진다")
    void createPolicy_Duplicate() {
        WelcomeBriefingPolicyDto request = new WelcomeBriefingPolicyDto(30, 60, 8.0, 70, true);
        given(coreTeamService.getTeamsByUser(1L, UserRole.NORMAL))
                .willReturn(List.of(new TeamResponse(3L, "3번팀", "설명", TeamRole.OWNER)));
        given(repository.existsByTeamIdAndRoomId(3L, 10L)).willReturn(true);

        assertThatThrownBy(() -> service.createPolicy(1L, UserRole.NORMAL, 3L, 10L, request))
                .isInstanceOf(PolicyDuplicateException.class);
    }

    @Test
    @DisplayName("확률 값이 0~100 범위를 벗어나면 예외를 던진다")
    void createPolicy_InvalidProbability() {
        WelcomeBriefingPolicyDto request = new WelcomeBriefingPolicyDto(-1, 60, 8.0, 70, true);
        given(coreTeamService.getTeamsByUser(1L, UserRole.NORMAL))
                .willReturn(List.of(new TeamResponse(3L, "3번팀", "설명", TeamRole.OWNER)));

        assertThatThrownBy(() -> service.createPolicy(1L, UserRole.NORMAL, 3L, 10L, request))
                .isInstanceOf(ProbabilityRangeException.class);
    }

    @Test
    @DisplayName("비 가능성 기준이 비 예상 기준보다 크면 예외를 던진다")
    void createPolicy_InvalidRange() {
        WelcomeBriefingPolicyDto request = new WelcomeBriefingPolicyDto(70, 60, 8.0, 70, true);
        given(coreTeamService.getTeamsByUser(1L, UserRole.NORMAL))
                .willReturn(List.of(new TeamResponse(3L, "3번팀", "설명", TeamRole.OWNER)));

        assertThatThrownBy(() -> service.createPolicy(1L, UserRole.NORMAL, 3L, 10L, request))
                .isInstanceOf(InvalidPolicyRangeException.class);
    }

    @Test
    @DisplayName("정책을 수정하면 조회한 엔티티 값이 변경된다")
    void updatePolicy() {
        WelcomeBriefingPolicy entity = new WelcomeBriefingPolicy(3L, 10L, 30, 60, 8.0, 70, true);
        WelcomeBriefingPolicyDto request = new WelcomeBriefingPolicyDto(25, 55, 7.0, 65, false);
        given(coreTeamService.getTeamsByUser(1L, UserRole.NORMAL))
                .willReturn(List.of(new TeamResponse(3L, "3번팀", "설명", TeamRole.ADMIN)));
        given(repository.findByTeamIdAndRoomId(3L, 10L)).willReturn(Optional.of(entity));

        WelcomeBriefingPolicy result = service.updatePolicy(1L, UserRole.NORMAL, 3L, 10L, request);

        assertThat(result.getRainPossibleProbability()).isEqualTo(25);
        assertThat(result.getRainExpectedProbability()).isEqualTo(55);
        assertThat(result.getStrongWindSpeed()).isEqualTo(7.0);
        assertThat(result.getHighHumidityPercent()).isEqualTo(65);
        assertThat(result.getEnabled()).isFalse();
    }

    @Test
    @DisplayName("정책 활성화 여부를 수정한다")
    void updatePolicyEnabled() {
        WelcomeBriefingPolicy entity = new WelcomeBriefingPolicy(3L, 10L, 30, 60, 8.0, 70, true);
        given(coreTeamService.getTeamsByUser(1L, UserRole.NORMAL))
                .willReturn(List.of(new TeamResponse(3L, "3번팀", "설명", TeamRole.ADMIN)));
        given(repository.findByTeamIdAndRoomId(3L, 10L)).willReturn(Optional.of(entity));

        service.updatePolicyEnabled(1L, UserRole.NORMAL, 3L, 10L, false);

        assertThat(entity.getEnabled()).isFalse();
    }

    @Test
    @DisplayName("정책을 실제 삭제한다")
    void deletePolicy() {
        WelcomeBriefingPolicy entity = new WelcomeBriefingPolicy(3L, 10L, 30, 60, 8.0, 70, true);
        given(coreTeamService.getTeamsByUser(1L, UserRole.NORMAL))
                .willReturn(List.of(new TeamResponse(3L, "3번팀", "설명", TeamRole.ADMIN)));
        given(repository.findByTeamIdAndRoomId(3L, 10L)).willReturn(Optional.of(entity));

        service.deletePolicy(1L, UserRole.NORMAL, 3L, 10L);

        verify(repository).delete(entity);
    }
}
