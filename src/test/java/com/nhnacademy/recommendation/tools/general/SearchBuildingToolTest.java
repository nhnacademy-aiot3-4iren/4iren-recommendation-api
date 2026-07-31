package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.PageResponse;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.building.BuildingResponse;
import com.nhnacademy.recommendation.dto.llm.LlmConversationContext;
import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityType;
import com.nhnacademy.recommendation.dto.tool.ToolResult;
import com.nhnacademy.recommendation.service.LlmConversationContextService;
import com.nhnacademy.recommendation.service.MentionedEntityResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class SearchBuildingToolTest {

    @Mock
    CoreClient coreClient;

    @Mock
    LlmRequestContextHolder contextHolder;

    @Mock
    LlmConversationContextService conversationContextService;

    @Mock
    MentionedEntityResolver mentionedEntityResolver;

    @Test
    @DisplayName("팀 ID가 있으면 Core API로 건물 목록을 조회하고 성공 ToolResult를 반환한다")
    void getBuildingListByTeamSuccess() {
        SearchBuildingTool tool = new SearchBuildingTool(coreClient, contextHolder, conversationContextService, mentionedEntityResolver);
        LlmRequestContext context = new LlmRequestContext(1L, UserRole.NORMAL, LlmConversationContext.empty());
        List<BuildingResponse> buildings = List.of(new BuildingResponse(10L, 3L, "본관", "본관 설명"));

        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(coreClient.getBuildingListByTeam(1L, UserRole.NORMAL, 3L))
                .willReturn(new PageResponse<>(buildings, 0, 10, 1, 1, true, true));

        ToolResult<List<BuildingResponse>> result = tool.getBuildingListByTeam(3L);

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo("SUCCESS");
        assertThat(result.data()).containsExactlyElementsOf(buildings);
        verify(conversationContextService).saveBuildingListMentions(1L, 3L);
    }

    @Test
    @DisplayName("팀 ID를 해석하지 못하면 Core API를 호출하지 않고 실패 ToolResult를 반환한다")
    void getBuildingListByTeamMissingTeamId() {
        SearchBuildingTool tool = new SearchBuildingTool(coreClient, contextHolder, conversationContextService, mentionedEntityResolver);

        given(contextHolder.get()).willReturn(new LlmRequestContext(1L, UserRole.NORMAL, LlmConversationContext.empty()));
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.TEAM)).willReturn(null);

        ToolResult<List<BuildingResponse>> result = tool.getBuildingListByTeam(null);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("MISSING_TEAM_ID");
        assertThat(result.data()).isNull();
        verifyNoInteractions(coreClient);
        verifyNoMoreInteractions(conversationContextService);
    }
}
