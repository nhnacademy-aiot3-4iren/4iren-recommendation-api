package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityType;
import com.nhnacademy.recommendation.dto.room.RoomResponse;
import com.nhnacademy.recommendation.service.LlmConversationContextService;
import com.nhnacademy.recommendation.service.MentionedEntityResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchRoomTool {
    private final CoreClient coreClient;
    private final LlmRequestContextHolder llmRequestContextHolder;
    private final LlmConversationContextService llmConversationContextService;
    private final MentionedEntityResolver mentionedEntityResolver;

    @Tool(
            name = "search_room_list",
            description = """
                    팀 ID와 건물 ID로 건물 내 강의실 목록을 조회합니다.
                    예: "3번팀 5번 건물 강의실 목록", "그 건물 강의실 보여줘"라는 질문에는 반드시 이 도구를 호출하세요.
                    """
    )
    public List<RoomResponse> getRoomListByBuilding(
            @ToolParam(required = false, description = "팀의 번호. 현재 질문에 팀 번호가 없으면 생략하세요.") Long teamId,
            @ToolParam(required = false, description = "건물 번호. 현재 질문에 건물 번호가 없으면 생략하세요.") Long buildingId) {
        long start = System.currentTimeMillis();
        LlmRequestContext context = llmRequestContextHolder.get();
        Long resolvedTeamId = mentionedEntityResolver.resolve(teamId, MentionedEntityType.TEAM);
        Long resolvedBuildingId = mentionedEntityResolver.resolve(buildingId, MentionedEntityType.BUILDING);
        if (resolvedTeamId == null || resolvedBuildingId == null) {
            log.info("[SearchRoomTool] 팀 번호 또는 건물 번호가 없어 강의실 목록 조회를 중단합니다.");
            return List.of(new RoomResponse(null, resolvedBuildingId, "조회 조건 부족", "팀 번호와 건물 번호가 필요합니다. 어느 팀의 어떤 건물인지 물어보세요."));
        }

        log.info("[SearchRoomTool] 건물 내 강의실 목록 조회 호출 TeamID: {}, BuildingID: {}", resolvedTeamId, resolvedBuildingId);

        List<RoomResponse> result = null;
        try{
            result = coreClient.getRoomListByBuilding(context.userId(), context.role(), resolvedTeamId, resolvedBuildingId).content();
            llmConversationContextService.saveRoomListMentions(context.userId(), resolvedTeamId, resolvedBuildingId);
        } catch (Exception e) {
            log.info("현재 구현되지 않은 도구 호출입니다. [SearchRoomTool]");
        } finally {
            log.info("[Timing][Tool] search_room_list teamId={} buildingId={} elapsed={}ms",
                    resolvedTeamId, resolvedBuildingId, System.currentTimeMillis() - start);
        }
        return result;
    }

    @Tool(
            name = "search_room_detail",
            description = """
                    팀 ID와 강의실 ID로 강의실 상세 정보를 조회합니다.
                    예: "3번팀 10번 강의실 상세", "그 강의실 정보 보여줘"라는 질문에는 반드시 이 도구를 호출하세요.
                    """
    )
    public RoomResponse getRoomDetail(
            @ToolParam(required = false, description = "팀의 번호. 현재 질문에 팀 번호가 없으면 생략하세요.") Long teamId,
            @ToolParam(required = false, description = "강의실 번호. 현재 질문에 강의실 번호가 없으면 생략하세요.") Long roomId) {
        long start = System.currentTimeMillis();
        LlmRequestContext context = llmRequestContextHolder.get();
        Long resolvedTeamId = mentionedEntityResolver.resolve(teamId, MentionedEntityType.TEAM);
        Long resolvedRoomId = mentionedEntityResolver.resolve(roomId, MentionedEntityType.ROOM);
        if (resolvedTeamId == null || resolvedRoomId == null) {
            log.info("[SearchRoomTool] 팀 번호 또는 강의실 번호가 없어 강의실 상세 조회를 중단합니다.");
            return new RoomResponse(
                    resolvedRoomId,
                    null,
                    "조회 조건 부족",
                    "팀 번호와 강의실 번호가 필요합니다. 어느 팀의 어떤 강의실인지 물어보세요."
            );
        }

        log.info("[SearchRoomTool] 강의실 상세 정보 조회 호출 TeamID: {}, RoomID: {}", resolvedTeamId, resolvedRoomId);
        try{
            RoomResponse response = coreClient.getRoomDetail(context.userId(), context.role(), resolvedTeamId, resolvedRoomId);
            llmConversationContextService.saveRoomDetailMentions(context.userId(), resolvedTeamId, response);
            return response;
        } catch (Exception e) {
            log.info("현재 구현되지 않은 도구 호출입니다. [SearchBuildingTool]");
            return new RoomResponse(resolvedRoomId, 1L, "오류로 발생한 강의실", "오류로 발생한 강의실 설명");
        } finally {
            log.info("[Timing][Tool] search_room_detail teamId={} roomId={} elapsed={}ms",
                    resolvedTeamId, resolvedRoomId, System.currentTimeMillis() - start);
        }
    }

}
