package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityType;
import com.nhnacademy.recommendation.dto.roomsub.RoomSubscriptionResponse;
import com.nhnacademy.recommendation.dto.tool.ToolResult;
import com.nhnacademy.recommendation.service.core.CoreSubscriptionRoomService;
import com.nhnacademy.recommendation.service.LlmConversationContextService;
import com.nhnacademy.recommendation.service.MentionedEntityResolver;
import com.nhnacademy.recommendation.util.TimingLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchSubscriptionRoomTool {
    private final CoreSubscriptionRoomService coreSubscriptionRoomService;
    private final LlmRequestContextHolder llmRequestContextHolder;
    private final LlmConversationContextService llmConversationContextService;
    private final MentionedEntityResolver mentionedEntityResolver;


    @Tool(
            name = "search_subscription_room_list_by_userid_and_teamid",
            description = """
                    팀ID로 사용자가 현재 구독중인 강의실(방) 목록을 조회합니다.
                    추후 팀의 강의실 목록 조회 도구를 호출하여 답변을 보완할 수 있습니다.
                    """
    )
    public ToolResult<List<RoomSubscriptionResponse>> getSubscriptionRoomsByUserAndTeam(
            @ToolParam(required = false, description = "팀의 번호. 현재 질문에 팀 번호가 없으면 생략하세요.") Long teamId){
        log.info("[SearchSubscriptionRoomTool] 현재 구독중인 강의실(Team) 목록 조회 호출");
        LlmRequestContext context = llmRequestContextHolder.get();
        Long resolvedTeamId = mentionedEntityResolver.resolve(teamId, MentionedEntityType.TEAM);
        return TimingLog.measure(log, "[Timing][Tool] search_subScription_room_list teamId=" + resolvedTeamId, ()->{
            if (resolvedTeamId == null) {
                log.info("[SearchSubscriptionRoomTool] 팀 번호가 없어 현재 구독중인 강의실(Team) 목록 조회를 중단합니다.");
                return ToolResult.failure("MISSING_TEAM_ID", "팀 번호가 필요합니다. 어느 팀의 강의실 구독 정보 목록인지 물어보세요.");
            }
            try {
                List<RoomSubscriptionResponse> result = coreSubscriptionRoomService.getSubscriptions(context.userId(), context.role(),resolvedTeamId);
                llmConversationContextService.saveTeamMention(context.userId(), resolvedTeamId);
                return ToolResult.success(result);
            } catch (Exception e) {
                log.warn("[SearchSubscriptionRoomTool] 강의실 구독 정보 목록 조회 실패. userId={}, teamId={}",context.userId(), resolvedTeamId, e);
                return ToolResult.failure("SUBSCRIPTION_ROOM_LIST_QUERY_FAILED", "구독 정보 목록을 조회하지 못했습니다.");
            }
        });

    }
}
