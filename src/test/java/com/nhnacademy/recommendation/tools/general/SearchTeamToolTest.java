package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.llm.LlmConversationContext;
import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import com.nhnacademy.recommendation.dto.team.TeamResponse;
import com.nhnacademy.recommendation.dto.team.TeamRole;
import com.nhnacademy.recommendation.dto.tool.ToolResult;
import com.nhnacademy.recommendation.service.CoreTeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
class SearchTeamToolTest {
    @Mock
    CoreTeamService coreTeamService;

    @Mock
    LlmRequestContextHolder contextHolder;

    SearchTeamTool tool;
    LlmRequestContext context;

    @BeforeEach
    void setUp() {
        context = new LlmRequestContext(1L, UserRole.NORMAL, LlmConversationContext.empty());
        tool = new SearchTeamTool(coreTeamService, contextHolder);
    }

    @Test
    @DisplayName("가입중인 팀 목록 조회")
    void getTeams(){
        List<TeamResponse> teams = List.of(new TeamResponse(3L, "3번팀", "3번팀 설명", TeamRole.MEMBER));

        given(contextHolder.get()).willReturn(context);
        given(coreTeamService.getTeamsByUser(1L, UserRole.NORMAL)).willReturn(teams);
        ToolResult<List<TeamResponse>> result = tool.getTeamsByUser();

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo("SUCCESS");
        assertThat(result.data()).containsExactlyElementsOf(teams);
    }

    @Test
    @DisplayName("가입중인 팀 목록 조회 - 가입 안함")
    void getTeams_No_Regist(){
        List<TeamResponse> teams = List.of();

        given(contextHolder.get()).willReturn(context);
        given(coreTeamService.getTeamsByUser(1L, UserRole.NORMAL)).willReturn(teams);
        ToolResult<List<TeamResponse>> result = tool.getTeamsByUser();

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo("SUCCESS");
        assertThat(result.data()).containsExactlyElementsOf(teams);
    }

    @Test
    @DisplayName("가입중인 팀 목록 조회 실패 - CoreAPI 오류")
    void getTeams_Fail(){
        given(contextHolder.get()).willReturn(context);
        given(coreTeamService.getTeamsByUser(1L, UserRole.NORMAL)).willThrow(new RuntimeException());

        ToolResult<List<TeamResponse>> result = tool.getTeamsByUser();

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("TEAM_LIST_QUERY_FAILED");
        assertThat(result.data()).isNull();

    }
}
