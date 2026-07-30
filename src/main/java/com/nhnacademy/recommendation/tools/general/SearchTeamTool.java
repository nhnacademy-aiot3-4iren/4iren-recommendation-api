package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.PageResponse;
import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import com.nhnacademy.recommendation.dto.team.TeamResponse;
import com.nhnacademy.recommendation.dto.tool.ToolResult;
import com.nhnacademy.recommendation.service.LlmConversationContextService;
import com.nhnacademy.recommendation.service.MentionedEntityResolver;
import com.nhnacademy.recommendation.util.TimingLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchTeamTool {

    private final CoreClient coreClient;
    private final LlmRequestContextHolder llmRequestContextHolder;
    private final LlmConversationContextService llmConversationContextService;
    private final MentionedEntityResolver mentionedEntityResolver;



    @Tool(
            name = "search_team_list",
            description = """
                    현재 유저가 가입한 팀 목록을 조회합니다.
                    """
    )
    public ToolResult<List<TeamResponse>> getTeamsByUser(){
        log.info("[SearchTeamTool] 현재 유저가 가입한 팀 목록 조회 호출");
        return TimingLog.measure(log, "[Timing][Tool] search_team_list", () -> {
            LlmRequestContext context = llmRequestContextHolder.get();
            try {
                PageResponse<TeamResponse> teamPage = coreClient.getTeamsByUser(context.userId(), context.role());
                List<TeamResponse> teamList = teamPage.content().stream().toList();
                if(teamList.isEmpty()){
                    log.info("UserId: {} 가 현재 가입한 팀이 없습니다.", context.userId());
                }
                return ToolResult.success(teamList);
            } catch (Exception e) {
                log.warn("[SearchTeamTool] 팀 목록 조회 실패. userId={}", context.userId(), e);
                return ToolResult.failure("TEAM_LIST_QUERY_FAILED", "가입한 팀 목록을 조회하지 못했습니다.");
            }
        });

    }
}
