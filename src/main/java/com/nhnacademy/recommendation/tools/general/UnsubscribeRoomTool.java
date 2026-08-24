package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityType;
import com.nhnacademy.recommendation.dto.roomsub.RoomSubResponse;
import com.nhnacademy.recommendation.dto.roomsub.RoomSubscriptionResponse;
import com.nhnacademy.recommendation.dto.roomsub.RoomUnsubscriptionToolResponse;
import com.nhnacademy.recommendation.dto.tool.ToolResult;
import com.nhnacademy.recommendation.service.LlmConversationContextService;
import com.nhnacademy.recommendation.service.MentionedEntityResolver;
import com.nhnacademy.recommendation.service.core.CoreSubscriptionRoomService;
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
public class UnsubscribeRoomTool {

    private final CoreSubscriptionRoomService coreSubscriptionRoomService;
    private final LlmRequestContextHolder llmRequestContextHolder;
    private final LlmConversationContextService llmConversationContextService;
    private final MentionedEntityResolver mentionedEntityResolver;

    @Tool(
            name = "unsubscribe_room",
            description = """
                    사용자가 강의실 구독 취소를 요청할 때 호출합니다.
                    강의실 ID가 현재 질문이나 최근 언급 엔티티에 있으면 해당 강의실 구독을 취소합니다.
                    강의실 ID가 없으면 현재 구독 중인 강의실 목록을 반환하여 사용자가 취소할 강의실을 고르게 합니다.
                    이미 구독 중이 아닌 강의실은 취소하지 않습니다.
                    """
    )
    public ToolResult<RoomUnsubscriptionToolResponse> unsubscribeRoom(
            @ToolParam(required = false, description = "팀의 번호. 현재 질문에 팀 번호가 없으면 생략하세요.") Long teamId,
            @ToolParam(required = false, description = "구독 취소할 강의실 번호. 현재 질문에 강의실 번호가 없으면 생략하세요.") Long roomId
    ) {
        LlmRequestContext context = llmRequestContextHolder.get();
        Long resolvedTeamId = mentionedEntityResolver.resolve(teamId, MentionedEntityType.TEAM);
        Long resolvedRoomId = mentionedEntityResolver.resolve(roomId, MentionedEntityType.ROOM);

        return TimingLog.measure(log,
                "[Timing][Tool] unsubscribe_room teamId=" + resolvedTeamId + " roomId=" + resolvedRoomId,
                () -> unsubscribeOrFindCandidates(context, resolvedTeamId, resolvedRoomId));
    }

    private ToolResult<RoomUnsubscriptionToolResponse> unsubscribeOrFindCandidates(
            LlmRequestContext context,
            Long teamId,
            Long roomId
    ) {
        ToolResult<RoomUnsubscriptionToolResponse> validation = validateTeamId(teamId);
        if (validation != null) {
            return validation;
        }

        List<RoomSubResponse> subscribedRooms;
        try {
            subscribedRooms = findSubscribedRooms(context, teamId);
        } catch (Exception e) {
            log.warn("[UnsubscribeRoomTool] 구독 중인 강의실 목록 조회 실패. userId={}, teamId={}",
                    context.userId(), teamId, e);
            return ToolResult.failure("SUBSCRIPTION_ROOM_LIST_QUERY_FAILED", "구독 중인 강의실 목록을 조회하지 못했습니다.");
        }

        if (roomId == null) {
            if (subscribedRooms.isEmpty()) {
                return ToolResult.failure("NO_SUBSCRIBED_ROOM", "현재 구독 중인 강의실이 없습니다.");
            }
            return ToolResult.success(RoomUnsubscriptionToolResponse.candidates(subscribedRooms));
        }
        if (roomId <= 0) {
            return ToolResult.failure("INVALID_ROOM_ID", "강의실 번호는 양수여야 합니다.");
        }
        if (subscribedRooms.stream().noneMatch(room -> roomId.equals(room.roomId()))) {
            return ToolResult.failure("NOT_SUBSCRIBED_ROOM", "구독 중인 강의실이 아니므로 구독 취소할 수 없습니다.");
        }

        try {
            coreSubscriptionRoomService.unsubscribeFromRoom(context.userId(), context.role(), teamId, roomId);
            llmConversationContextService.saveTeamMention(context.userId(), teamId);
            llmConversationContextService.saveRoomMention(context.userId(), roomId, null);
            return ToolResult.success(RoomUnsubscriptionToolResponse.unsubscribed(roomId));
        } catch (Exception e) {
            log.warn("[UnsubscribeRoomTool] 강의실 구독 취소 실패. userId={}, teamId={}, roomId={}",
                    context.userId(), teamId, roomId, e);
            return ToolResult.failure("ROOM_UNSUBSCRIPTION_FAILED", "강의실 구독을 취소하지 못했습니다.");
        }
    }

    private ToolResult<RoomUnsubscriptionToolResponse> validateTeamId(Long teamId) {
        if (teamId == null) {
            return ToolResult.failure("MISSING_TEAM_ID", "팀 번호가 필요합니다. 어느 팀의 강의실 구독을 취소할지 물어보세요.");
        }
        if (teamId <= 0) {
            return ToolResult.failure("INVALID_TEAM_ID", "팀 번호는 양수여야 합니다.");
        }
        return null;
    }

    private List<RoomSubResponse> findSubscribedRooms(LlmRequestContext context, Long teamId) {
        if (context.roomSubInfo() != null && !context.roomSubInfo().isEmpty()) {
            return context.roomSubInfo();
        }
        return coreSubscriptionRoomService.getSubscriptions(context.userId(), context.role(), teamId).stream()
                .map(this::toRoomSubResponse)
                .toList();
    }

    private RoomSubResponse toRoomSubResponse(RoomSubscriptionResponse subscription) {
        return new RoomSubResponse(subscription.roomId(), null, subscription.notificationEnabled());
    }
}
