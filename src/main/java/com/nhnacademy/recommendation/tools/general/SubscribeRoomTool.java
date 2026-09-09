package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityType;
import com.nhnacademy.recommendation.dto.room.RoomResponse;
import com.nhnacademy.recommendation.dto.roomsub.RoomSubResponse;
import com.nhnacademy.recommendation.dto.roomsub.RoomSubscriptionResponse;
import com.nhnacademy.recommendation.dto.roomsub.RoomSubscriptionToolResponse;
import com.nhnacademy.recommendation.dto.tool.ToolResult;
import com.nhnacademy.recommendation.service.LlmConversationContextService;
import com.nhnacademy.recommendation.service.MentionedEntityResolver;
import com.nhnacademy.recommendation.service.core.CoreRoomService;
import com.nhnacademy.recommendation.service.core.CoreSubscriptionRoomService;
import com.nhnacademy.recommendation.util.TimingLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscribeRoomTool {

    private final CoreSubscriptionRoomService coreSubscriptionRoomService;
    private final CoreRoomService coreRoomService;
    private final LlmRequestContextHolder llmRequestContextHolder;
    private final LlmConversationContextService llmConversationContextService;
    private final MentionedEntityResolver mentionedEntityResolver;

    @Tool(
            name = "subscribe_room",
            description = """
                    사용자가 강의실 구독을 요청할 때 호출합니다.
                    강의실 ID가 현재 질문이나 최근 언급 엔티티에 있으면 해당 강의실을 구독합니다.
                    강의실 ID가 없고 팀 ID와 건물 ID가 있으면 구독 가능한 강의실 후보 목록을 반환합니다.
                    이미 구독 중인 강의실은 다시 구독하지 않습니다.
                    """
    )
    public ToolResult<RoomSubscriptionToolResponse> subscribeRoom(
            @ToolParam(required = false, description = "팀의 번호. 현재 질문에 팀 번호가 없으면 생략하세요.") Long teamId,
            @ToolParam(required = false, description = "건물 번호. 구독 대상 강의실을 특정하지 못했을 때 후보 목록 조회에 사용합니다.") Long buildingId,
            @ToolParam(required = false, description = "구독할 강의실 번호. 현재 질문에 강의실 번호가 없으면 생략하세요.") Long roomId
    ) {
        LlmRequestContext context = llmRequestContextHolder.get();
        Long resolvedTeamId = mentionedEntityResolver.resolve(teamId, MentionedEntityType.TEAM);
        Long resolvedBuildingId = mentionedEntityResolver.resolve(buildingId, MentionedEntityType.BUILDING);
        Long resolvedRoomId = mentionedEntityResolver.resolve(roomId, MentionedEntityType.ROOM);

        return TimingLog.measure(log,
                "[Timing][Tool] subscribe_room teamId=" + resolvedTeamId
                        + " buildingId=" + resolvedBuildingId
                        + " roomId=" + resolvedRoomId,
                () -> subscribeOrFindCandidates(context, resolvedTeamId, resolvedBuildingId, resolvedRoomId));
    }

    private ToolResult<RoomSubscriptionToolResponse> subscribeOrFindCandidates(
            LlmRequestContext context,
            Long teamId,
            Long buildingId,
            Long roomId
    ) {
        ToolResult<RoomSubscriptionToolResponse> validation = validateTeamId(teamId);
        if (validation != null) {
            return validation;
        }

        if (roomId == null) {
            return findAvailableRooms(context, teamId, buildingId);
        }
        if (roomId <= 0) {
            return ToolResult.failure("INVALID_ROOM_ID", "강의실 번호는 양수여야 합니다.");
        }

        try {
            if (isAlreadySubscribed(context, teamId, roomId)) {
                return ToolResult.failure("ALREADY_SUBSCRIBED_ROOM", "이미 구독 중인 강의실입니다.");
            }
            RoomSubscriptionResponse subscription = coreSubscriptionRoomService.subscribeToRoom(
                    context.userId(),
                    context.role(),
                    teamId,
                    roomId
            );
            llmConversationContextService.saveTeamMention(context.userId(), teamId);
            llmConversationContextService.saveRoomMention(context.userId(), roomId, null);
            return ToolResult.success(RoomSubscriptionToolResponse.subscribed(subscription));
        } catch (Exception e) {
            log.warn("[SubscribeRoomTool] 강의실 구독 실패. userId={}, teamId={}, roomId={}",
                    context.userId(), teamId, roomId, e);
            return ToolResult.failure("ROOM_SUBSCRIPTION_FAILED", "강의실을 구독하지 못했습니다.");
        }
    }

    private ToolResult<RoomSubscriptionToolResponse> findAvailableRooms(
            LlmRequestContext context,
            Long teamId,
            Long buildingId
    ) {
        if (buildingId == null) {
            return ToolResult.failure("MISSING_ROOM_SUBSCRIPTION_TARGET",
                    "구독할 강의실을 특정할 수 없습니다. 어느 건물의 강의실을 구독할지 물어보세요.");
        }
        if (buildingId <= 0) {
            return ToolResult.failure("INVALID_BUILDING_ID", "건물 번호는 양수여야 합니다.");
        }

        try {
            List<RoomResponse> rooms = coreRoomService.getRoomListByBuilding(
                    context.userId(),
                    context.role(),
                    teamId,
                    buildingId
            );
            Set<Long> subscribedRoomIds = findSubscribedRoomIds(context, teamId);
            List<RoomResponse> availableRooms = rooms.stream()
                    .filter(room -> room.roomId() != null)
                    .filter(room -> !subscribedRoomIds.contains(room.roomId()))
                    .toList();
            llmConversationContextService.saveTeamMention(context.userId(), teamId);
            llmConversationContextService.saveBuildingMention(context.userId(), buildingId, null);
            return ToolResult.success(RoomSubscriptionToolResponse.candidates(availableRooms));
        } catch (Exception e) {
            log.warn("[SubscribeRoomTool] 구독 가능한 강의실 목록 조회 실패. userId={}, teamId={}, buildingId={}",
                    context.userId(), teamId, buildingId, e);
            return ToolResult.failure("AVAILABLE_ROOM_LIST_QUERY_FAILED", "구독 가능한 강의실 목록을 조회하지 못했습니다.");
        }
    }

    private ToolResult<RoomSubscriptionToolResponse> validateTeamId(Long teamId) {
        if (teamId == null) {
            return ToolResult.failure("MISSING_TEAM_ID", "팀 번호가 필요합니다. 어느 팀의 강의실을 구독할지 물어보세요.");
        }
        if (teamId <= 0) {
            return ToolResult.failure("INVALID_TEAM_ID", "팀 번호는 양수여야 합니다.");
        }
        return null;
    }

    private boolean isAlreadySubscribed(LlmRequestContext context, Long teamId, Long roomId) {
        return findSubscribedRoomIds(context, teamId).contains(roomId);
    }

    private Set<Long> findSubscribedRoomIds(LlmRequestContext context, Long teamId) {
        if (context.roomSubInfo() != null && !context.roomSubInfo().isEmpty()) {
            return context.roomSubInfo().stream()
                    .map(RoomSubResponse::roomId)
                    .collect(Collectors.toSet());
        }
        return coreSubscriptionRoomService.getSubscriptions(context.userId(), context.role(), teamId).stream()
                .map(RoomSubscriptionResponse::roomId)
                .collect(Collectors.toSet());
    }
}
