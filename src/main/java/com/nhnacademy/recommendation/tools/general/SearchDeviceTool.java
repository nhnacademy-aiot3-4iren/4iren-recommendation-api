
package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.device.DeviceResponse;
import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityType;
import com.nhnacademy.recommendation.dto.tool.ToolResult;
import com.nhnacademy.recommendation.service.LlmConversationContextService;
import com.nhnacademy.recommendation.service.MentionedEntityResolver;
import com.nhnacademy.recommendation.service.core.CoreDeviceService;
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
public class SearchDeviceTool {
    private final CoreDeviceService coreDeviceService;
    private final LlmRequestContextHolder llmRequestContextHolder;
    private final LlmConversationContextService llmConversationContextService;
    private final MentionedEntityResolver mentionedEntityResolver;

    @Tool(
            name = "search_device_list",
            description = """
                    팀 ID와 강의실 ID로 강의실 내 기기 목록을 조회합니다.
                    예: "3번팀 10번 강의실 기기 목록", "그 강의실 기기 보여줘"라는 질문에는 반드시 이 도구를 호출하세요.
                    """
    )
    public ToolResult<List<DeviceResponse>> getDeviceListByRoom(
            @ToolParam(required = false, description = "팀의 번호. 현재 질문에 팀 번호가 없으면 생략하세요.") Long teamId,
            @ToolParam(required = false, description = "강의실 번호. 현재 질문에 강의실 번호가 없으면 생략하세요.") Long roomId
    ) {
        LlmRequestContext context = llmRequestContextHolder.get();
        Long resolvedTeamId = mentionedEntityResolver.resolve(teamId, MentionedEntityType.TEAM);
        Long resolvedRoomId = mentionedEntityResolver.resolve(roomId, MentionedEntityType.ROOM);
        return TimingLog.measure(log,
                "[Timing][Tool] search_device_list teamId=" + resolvedTeamId + " roomId=" + resolvedRoomId,
                () -> {
                    if (resolvedTeamId == null || resolvedRoomId == null) {
                        log.info("[SearchDeviceTool] 팀 번호 또는 강의실 번호가 없어 기기 목록 조회를 중단합니다.");
                        return ToolResult.failure("MISSING_DEVICE_LIST_CONDITION",
                                "팀 번호와 강의실 번호가 필요합니다. 어느 팀의 어떤 강의실인지 물어보세요.");
                    }

                    log.info("[SearchDeviceTool] 강의실 내 기기 목록 조회 호출 TeamID: {}, RoomID: {}", resolvedTeamId, resolvedRoomId);

                    try {
                        List<DeviceResponse> result = coreDeviceService.getDeviceListByRoom(context.userId(), context.role(), resolvedTeamId, resolvedRoomId);
                        llmConversationContextService.saveTeamMention(context.userId(), resolvedTeamId);
                        llmConversationContextService.saveRoomMention(context.userId(), resolvedRoomId, null);
                        return ToolResult.success(result);
                    } catch (Exception e) {
                        log.warn("[SearchDeviceTool] 강의실 내 기기 목록 조회 실패. teamId={}, roomId={}", resolvedTeamId, resolvedRoomId, e);
                        return ToolResult.failure("DEVICE_LIST_QUERY_FAILED", "기기 목록을 조회하지 못했습니다.");
                    }
                }
        );
    }
}
