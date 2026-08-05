package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.building.BuildingDetailResponse;
import com.nhnacademy.recommendation.dto.building.BuildingResponse;
import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityType;
import com.nhnacademy.recommendation.dto.tool.ToolResult;
import com.nhnacademy.recommendation.service.CoreBuildingService;
import com.nhnacademy.recommendation.service.LlmConversationContextService;
import com.nhnacademy.recommendation.service.MentionedEntityResolver;
import com.nhnacademy.recommendation.util.TimingLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchBuildingTool {

    private final CoreBuildingService coreBuildingService;
    private final LlmRequestContextHolder llmRequestContextHolder;
    private final LlmConversationContextService llmConversationContextService;
    private final MentionedEntityResolver mentionedEntityResolver;


    @Tool(
            name = "search_building_list",
            description = """
                    팀 ID로 사용자가 접근 가능한 건물 목록을 조회합니다.
                    예: "3번팀 건물 목록", "3번 팀이 관리하는 건물 보여줘"라는 질문에는 반드시 이 도구를 호출하세요.
                    """
    )
    public ToolResult<List<BuildingResponse>> getBuildingListByTeam(
            @ToolParam(required = false, description = "팀의 번호. 현재 질문에 팀 번호가 없으면 생략하세요.") Long teamId) {
        log.info("[SearchBuildingTool] 팀의 관리대상 건물 목록 조회 호출");
        LlmRequestContext context = llmRequestContextHolder.get();
        Long resolvedTeamId = mentionedEntityResolver.resolve(teamId, MentionedEntityType.TEAM);
        return TimingLog.measure(log, "[Timing][Tool] search_building_list teamId=" + resolvedTeamId, () -> {
            if (resolvedTeamId == null) {
                log.info("[SearchBuildingTool] 팀 번호가 없어 건물 목록 조회를 중단합니다.");
                return ToolResult.failure("MISSING_TEAM_ID", "팀 번호가 필요합니다. 어느 팀의 건물 목록인지 물어보세요.");
            }
            try {
                List<BuildingResponse> result = coreBuildingService.getBuildingList(context.userId(), context.role(), resolvedTeamId);
                llmConversationContextService.saveBuildingListMentions(context.userId(), resolvedTeamId);
                return ToolResult.success(result);
            } catch (Exception e) {
                log.warn("[SearchBuildingTool] 건물 목록 조회 실패. teamId={}", resolvedTeamId, e);
                return ToolResult.failure("BUILDING_LIST_QUERY_FAILED", "건물 목록을 조회하지 못했습니다.");
            }
        });
    }

    @Tool(
            name = "search_building_detail",
            description = """
                    팀 ID와 건물 ID로 건물 상세 정보를 조회합니다.
                    예: "3번팀 5번 건물 상세", "그 건물 상세정보"라는 질문에는 반드시 이 도구를 호출하세요.
                    """
    )
    public ToolResult<BuildingDetailResponse> getBuildingDetail(
            @ToolParam(required = false, description = "팀의 번호. 현재 질문에 팀 번호가 없으면 생략하세요.") Long teamId,
            @ToolParam(required = false, description = "건물 번호. 현재 질문에 건물 번호가 없으면 생략하세요.") Long buildingId) {
        LlmRequestContext context = llmRequestContextHolder.get();
        Long resolvedTeamId = mentionedEntityResolver.resolve(teamId, MentionedEntityType.TEAM);
        Long resolvedBuildingId = mentionedEntityResolver.resolve(buildingId, MentionedEntityType.BUILDING);
        return TimingLog.measure(log,
                "[Timing][Tool] search_building_detail teamId=" + resolvedTeamId + " buildingId=" + resolvedBuildingId,
                () -> {
                    if (resolvedTeamId == null || resolvedBuildingId == null) {
                        log.info("[SearchBuildingTool] 팀 번호 또는 건물 번호가 없어 건물 상세 조회를 중단합니다.");
                        return ToolResult.failure("MISSING_BUILDING_DETAIL_CONDITION",
                                "팀 번호와 건물 번호가 필요합니다. 어느 팀의 어떤 건물인지 물어보세요.");
                    }

                    log.info("[SearchBuildingTool] 건물 상세 정보 조회 호출 Team ID: {}, Building ID: {}", resolvedTeamId, resolvedBuildingId);
                    try {
                        BuildingDetailResponse response = coreBuildingService.getBuildingDetail(
                                context.userId(),
                                context.role(),
                                resolvedTeamId,
                                resolvedBuildingId
                        );
                        llmConversationContextService.saveBuildingDetailMentions(context.userId(), resolvedTeamId, response);
                        return ToolResult.success(response);
                    } catch (Exception e) {
                        log.warn("[SearchBuildingTool] 건물 상세 조회 실패. teamId={}, buildingId={}", resolvedTeamId, resolvedBuildingId, e);
                        return ToolResult.failure("BUILDING_DETAIL_QUERY_FAILED", "건물 상세 정보를 조회하지 못했습니다.");
                    }
                });
    }

}
