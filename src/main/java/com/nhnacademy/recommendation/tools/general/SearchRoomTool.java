package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityDto;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityType;
import com.nhnacademy.recommendation.dto.room.RoomResponse;
import com.nhnacademy.recommendation.service.LlmConversationContextService;
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

    @Tool(
            name = "search_room_list_by_buildingId",
            description = """
                    Team ID와 Building ID를 통해 건물 내 강의실 리스트를 조회합니다.
                    """
    )
    public List<RoomResponse> getRoomListByBuilding(
            @ToolParam(required = false, description = "팀의 번호. 현재 질문에 팀 번호가 없으면 생략하세요.") Long teamId,
            @ToolParam(required = false, description = "건물 번호. 현재 질문에 건물 번호가 없으면 생략하세요.") Long buildingId) {
        LlmRequestContext context = llmRequestContextHolder.get();
        Long resolvedTeamId = resolveEntityId(teamId, MentionedEntityType.TEAM);
        Long resolvedBuildingId = resolveEntityId(buildingId, MentionedEntityType.BUILDING);
        if (resolvedTeamId == null || resolvedBuildingId == null) {
            log.info("[SearchRoomTool] 팀 번호 또는 건물 번호가 없어 강의실 목록 조회를 중단합니다.");
            return List.of(new RoomResponse(null, resolvedBuildingId, "조회 조건 부족", "팀 번호와 건물 번호가 필요합니다. 어느 팀의 어떤 건물인지 물어보세요."));
        }

        log.info("[SearchRoomTool] 건물 내 강의실 목록 조회 호출 TeamID: {}, BuildingID: {}", resolvedTeamId, resolvedBuildingId);

        List<RoomResponse> result = null;
        try{
            result = coreClient.getRoomListByBuilding(resolvedTeamId, resolvedBuildingId).content();
            llmConversationContextService.saveMention(
                    context.userId(),
                    new MentionedEntityDto(MentionedEntityType.TEAM, resolvedTeamId, null)
            );
            llmConversationContextService.saveMention(
                    context.userId(),
                    new MentionedEntityDto(MentionedEntityType.BUILDING, resolvedBuildingId, null)
            );
        } catch (Exception e) {
            log.info("현재 구현되지 않은 도구 호출입니다. [SearchRoomTool]");
        }
        return result;
    }

    @Tool(
            name = "search_room_detail_by_roomId",
            description = """
                    TeamId와 RoomId를 통해 해당하는 강의실 세부 정보를 조회합니다.
                    """
    )
    public RoomResponse getRoomDetail(
            @ToolParam(required = false, description = "팀의 번호. 현재 질문에 팀 번호가 없으면 생략하세요.") Long teamId,
            @ToolParam(required = false, description = "강의실 번호. 현재 질문에 강의실 번호가 없으면 생략하세요.") Long roomId) {
        LlmRequestContext context = llmRequestContextHolder.get();
        Long resolvedTeamId = resolveEntityId(teamId, MentionedEntityType.TEAM);
        Long resolvedRoomId = resolveEntityId(roomId, MentionedEntityType.ROOM);
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
            RoomResponse response = coreClient.getRoomDetail(resolvedTeamId, resolvedRoomId);
            llmConversationContextService.saveMention(
                    context.userId(),
                    new MentionedEntityDto(MentionedEntityType.TEAM, resolvedTeamId, null)
            );
            llmConversationContextService.saveMention(
                    context.userId(),
                    new MentionedEntityDto(MentionedEntityType.BUILDING, response.buildingId(), null)
            );
            llmConversationContextService.saveMention(
                    context.userId(),
                    new MentionedEntityDto(MentionedEntityType.ROOM, response.roomId(), response.roomName())
            );
            return response;
        } catch (Exception e) {
            log.info("현재 구현되지 않은 도구 호출입니다. [SearchBuildingTool]");
            return new RoomResponse(resolvedRoomId, 1L, "오류로 발생한 강의실", "오류로 발생한 강의실 설명");
        }
    }

    private Long resolveEntityId(Long explicitId, MentionedEntityType type) {
        if (explicitId != null) {
            return explicitId;
        }
        LlmRequestContext context = llmRequestContextHolder.get();
        return context.conversationContext()
                .findRecentEntityId(type)
                .or(() -> llmConversationContextService.find(context.userId()).findRecentEntityId(type))
                .orElse(null);
    }
}
