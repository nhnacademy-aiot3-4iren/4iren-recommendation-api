package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.device.DeviceResponse;
import com.nhnacademy.recommendation.dto.llm.LlmConversationContext;
import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityType;
import com.nhnacademy.recommendation.dto.tool.ToolResult;
import com.nhnacademy.recommendation.service.LlmConversationContextService;
import com.nhnacademy.recommendation.service.MentionedEntityResolver;
import com.nhnacademy.recommendation.service.core.CoreDeviceService;
import org.junit.jupiter.api.BeforeEach;
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

@ExtendWith(MockitoExtension.class)
class SearchDeviceToolTest {

    @Mock
    CoreDeviceService coreDeviceService;

    @Mock
    LlmRequestContextHolder contextHolder;

    @Mock
    LlmConversationContextService conversationContextService;

    @Mock
    MentionedEntityResolver mentionedEntityResolver;

    SearchDeviceTool tool;
    LlmRequestContext context;

    @BeforeEach
    void setUp() {
        context = new LlmRequestContext(1L, UserRole.NORMAL, LlmConversationContext.empty());
        tool = new SearchDeviceTool(coreDeviceService, contextHolder, conversationContextService, mentionedEntityResolver);
    }

    @Test
    @DisplayName("강의실 내 기기 목록 조회 성공")
    void getDeviceListByRoom() {
        List<DeviceResponse> devices = List.of(new DeviceResponse(1L, 20L, "환기장치"));

        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(20L, MentionedEntityType.ROOM)).willReturn(20L);
        given(coreDeviceService.getDeviceListByRoom(1L, UserRole.NORMAL, 3L, 20L)).willReturn(devices);

        ToolResult<List<DeviceResponse>> result = tool.getDeviceListByRoom(3L, 20L);

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo("SUCCESS");
        assertThat(result.data()).containsExactlyElementsOf(devices);
        verify(conversationContextService).saveTeamMention(1L, 3L);
        verify(conversationContextService).saveRoomMention(1L, 20L, null);
    }

    @Test
    @DisplayName("강의실 내 기기 목록 조회 실패 - 팀 ID 누락")
    void getDeviceListByRoom_Fail_TeamId() {
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.TEAM)).willReturn(null);
        given(mentionedEntityResolver.resolve(20L, MentionedEntityType.ROOM)).willReturn(20L);

        ToolResult<List<DeviceResponse>> result = tool.getDeviceListByRoom(null, 20L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("MISSING_DEVICE_LIST_CONDITION");
        assertThat(result.data()).isNull();
        verifyNoInteractions(coreDeviceService);
        verifyNoInteractions(conversationContextService);
    }

    @Test
    @DisplayName("강의실 내 기기 목록 조회 실패 - 강의실 ID 누락")
    void getDeviceListByRoom_Fail_RoomId() {
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.ROOM)).willReturn(null);

        ToolResult<List<DeviceResponse>> result = tool.getDeviceListByRoom(3L, null);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("MISSING_DEVICE_LIST_CONDITION");
        assertThat(result.data()).isNull();
        verifyNoInteractions(coreDeviceService);
        verifyNoInteractions(conversationContextService);
    }

    @Test
    @DisplayName("강의실 내 기기 목록 조회 실패 - Core API 오류")
    void getDeviceListByRoom_Fail_CoreApi() {
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(20L, MentionedEntityType.ROOM)).willReturn(20L);
        given(coreDeviceService.getDeviceListByRoom(1L, UserRole.NORMAL, 3L, 20L)).willThrow(new RuntimeException());

        ToolResult<List<DeviceResponse>> result = tool.getDeviceListByRoom(3L, 20L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("DEVICE_LIST_QUERY_FAILED");
        assertThat(result.data()).isNull();
        verifyNoInteractions(conversationContextService);
    }
}
