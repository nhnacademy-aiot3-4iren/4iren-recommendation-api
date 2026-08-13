package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.building.BuildingDetailResponse;
import com.nhnacademy.recommendation.dto.building.BuildingResponse;
import com.nhnacademy.recommendation.dto.llm.LlmConversationContext;
import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityType;
import com.nhnacademy.recommendation.dto.tool.ToolResult;
import com.nhnacademy.recommendation.service.LlmConversationContextService;
import com.nhnacademy.recommendation.service.MentionedEntityResolver;
import com.nhnacademy.recommendation.service.core.CoreBuildingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchBuildingToolTest {

    @Mock
    CoreBuildingService coreBuildingService;

    @Mock
    LlmRequestContextHolder contextHolder;

    @Mock
    LlmConversationContextService conversationContextService;

    @Mock
    MentionedEntityResolver mentionedEntityResolver;

    SearchBuildingTool tool;
    LlmRequestContext context;

    @BeforeEach
    void setUp() {
        context = new LlmRequestContext(1L, UserRole.NORMAL, LlmConversationContext.empty());
        tool = new SearchBuildingTool(coreBuildingService, contextHolder, conversationContextService, mentionedEntityResolver);
    }

    @Test
    @DisplayName("팀 ID가 있으면 Core API로 건물 목록을 조회하고 성공 ToolResult를 반환한다")
    void getBuildingListByTeamSuccess() {
        List<BuildingResponse> buildings = List.of(new BuildingResponse(10L, 3L, "본관", "본관 설명", "도로명주소", "상세주소", "광주"));

        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(coreBuildingService.getBuildingList(1L, UserRole.NORMAL, 3L)).willReturn(buildings);

        ToolResult<List<BuildingResponse>> result = tool.getBuildingListByTeam(3L);

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo("SUCCESS");
        assertThat(result.data()).containsExactlyElementsOf(buildings);
        verify(conversationContextService).saveBuildingListMentions(1L, 3L);
    }

    @Test
    @DisplayName("팀 ID를 해석하지 못하면 Core API를 호출하지 않고 실패 ToolResult를 반환한다")
    void getBuildingListByTeamMissingTeamId() {
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.TEAM)).willReturn(null);

        ToolResult<List<BuildingResponse>> result = tool.getBuildingListByTeam(null);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("MISSING_TEAM_ID");
        assertThat(result.data()).isNull();
        verifyNoInteractions(coreBuildingService);
        verifyNoMoreInteractions(conversationContextService);
    }

    @Test
    @DisplayName("건물 리스트 조회 시 Core API에서 예외 발생 시 실패 ToolResult를 반환한다")
    void getBuildingListByCoreAPI_Exception() {
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        when(coreBuildingService.getBuildingList(1L, UserRole.NORMAL, 3L)).thenThrow(new RuntimeException());

        ToolResult<List<BuildingResponse>> result = tool.getBuildingListByTeam(3L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("BUILDING_LIST_QUERY_FAILED");
        assertThat(result.data()).isNull();
    }

    @Test
    @DisplayName("Core API로 건물 세부정보 조회 후 성공 ToolResult를 반환한다")
    void getBuildingDetail() {
        BuildingDetailResponse building = new BuildingDetailResponse(10L, 3L, "본관", "본관 설명", null, null, null, 0L, 0L, 0L);

        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(10L, MentionedEntityType.BUILDING)).willReturn(10L);
        given(coreBuildingService.getBuildingDetail(1L, UserRole.NORMAL, 3L, 10L))
                .willReturn(building);

        ToolResult<BuildingDetailResponse> result = tool.getBuildingDetail(3L, 10L);

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo("SUCCESS");
        assertThat(result.data()).isEqualTo(building);
        verify(conversationContextService).saveBuildingDetailMentions(1L, 3L, building);

    }

    @Test
    @DisplayName("팀ID 에러로 인한 실패 ToolResult 반환")
    void getBuildingDetail_Fail_TeamID(){
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.TEAM)).willReturn(null);

        ToolResult<BuildingDetailResponse> result = tool.getBuildingDetail(null, 10L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("MISSING_BUILDING_DETAIL_CONDITION");
        assertThat(result.data()).isNull();
        verifyNoInteractions(coreBuildingService);
        verifyNoMoreInteractions(conversationContextService);

    }

    @Test
    @DisplayName("빌딩ID 에러로 인한 실패 ToolResult 반환")
    void getBuildingDetail_Fail_BuildingID(){
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.BUILDING)).willReturn(null);

        ToolResult<BuildingDetailResponse> result = tool.getBuildingDetail(3L, null);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("MISSING_BUILDING_DETAIL_CONDITION");
        assertThat(result.data()).isNull();
        verifyNoInteractions(coreBuildingService);
        verifyNoMoreInteractions(conversationContextService);

    }

    @Test
    @DisplayName("건물 상세정보 조회 시 Core API에서 예외 발생 시 실패 ToolResult를 반환한다")
    void getBuildingDetailByCoreAPI_Exception() {
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(10L, MentionedEntityType.BUILDING)).willReturn(10L);
        when(coreBuildingService.getBuildingDetail(1L, UserRole.NORMAL, 3L, 10L)).thenThrow(new RuntimeException());

        ToolResult<BuildingDetailResponse> result = tool.getBuildingDetail(3L,10L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("BUILDING_DETAIL_QUERY_FAILED");
        assertThat(result.data()).isNull();
    }
}
