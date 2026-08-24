package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.llm.LlmConversationContext;
import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityType;
import com.nhnacademy.recommendation.dto.roomsub.RoomSubResponse;
import com.nhnacademy.recommendation.dto.roomsub.RoomSubscriptionResponse;
import com.nhnacademy.recommendation.dto.roomsub.RoomUnsubscriptionToolResponse;
import com.nhnacademy.recommendation.dto.tool.ToolResult;
import com.nhnacademy.recommendation.service.LlmConversationContextService;
import com.nhnacademy.recommendation.service.MentionedEntityResolver;
import com.nhnacademy.recommendation.service.core.CoreSubscriptionRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UnsubscribeRoomToolTest {

    @Mock
    CoreSubscriptionRoomService coreSubscriptionRoomService;

    @Mock
    LlmRequestContextHolder contextHolder;

    @Mock
    LlmConversationContextService conversationContextService;

    @Mock
    MentionedEntityResolver mentionedEntityResolver;

    UnsubscribeRoomTool tool;
    LlmRequestContext context;

    @BeforeEach
    void setUp() {
        context = new LlmRequestContext(1L, UserRole.NORMAL, LlmConversationContext.empty());
        tool = new UnsubscribeRoomTool(
                coreSubscriptionRoomService,
                contextHolder,
                conversationContextService,
                mentionedEntityResolver
        );
    }

    @Test
    @DisplayName("강의실 ID가 있으면 구독 여부 확인 후 구독을 취소한다")
    void unsubscribeRoom() {
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(20L, MentionedEntityType.ROOM)).willReturn(20L);
        given(coreSubscriptionRoomService.getSubscriptions(1L, UserRole.NORMAL, 3L))
                .willReturn(List.of(new RoomSubscriptionResponse(10L, 20L, true)));
        willDoNothing().given(coreSubscriptionRoomService).unsubscribeFromRoom(1L, UserRole.NORMAL, 3L, 20L);

        ToolResult<RoomUnsubscriptionToolResponse> result = tool.unsubscribeRoom(3L, 20L);

        assertThat(result.success()).isTrue();
        assertThat(result.data().unsubscribedRoomId()).isEqualTo(20L);
        assertThat(result.data().subscribedRooms()).isEmpty();
        verify(coreSubscriptionRoomService).unsubscribeFromRoom(1L, UserRole.NORMAL, 3L, 20L);
        verify(conversationContextService).saveTeamMention(1L, 3L);
        verify(conversationContextService).saveRoomMention(1L, 20L, null);
    }

    @Test
    @DisplayName("요청 컨텍스트 구독 목록을 제공받으면 Core 목록 조회 없이 구독을 취소한다")
    void unsubscribeRoom_UseContextSubscriptions() {
        LlmRequestContext contextWithSubscriptions = new LlmRequestContext(
                1L,
                UserRole.NORMAL,
                LlmConversationContext.empty(),
                null,
                List.of(new RoomSubResponse(20L, "201호", true))
        );

        given(contextHolder.get()).willReturn(contextWithSubscriptions);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(20L, MentionedEntityType.ROOM)).willReturn(20L);
        willDoNothing().given(coreSubscriptionRoomService).unsubscribeFromRoom(1L, UserRole.NORMAL, 3L, 20L);

        ToolResult<RoomUnsubscriptionToolResponse> result = tool.unsubscribeRoom(3L, 20L);

        assertThat(result.success()).isTrue();
        assertThat(result.data().unsubscribedRoomId()).isEqualTo(20L);
        verify(coreSubscriptionRoomService, never()).getSubscriptions(1L, UserRole.NORMAL, 3L);
        verify(coreSubscriptionRoomService).unsubscribeFromRoom(1L, UserRole.NORMAL, 3L, 20L);
    }

    @Test
    @DisplayName("강의실 ID가 없으면 구독 중인 강의실 후보를 반환한다")
    void unsubscribeRoom_ReturnSubscribedRoomCandidates() {
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.ROOM)).willReturn(null);
        given(coreSubscriptionRoomService.getSubscriptions(1L, UserRole.NORMAL, 3L))
                .willReturn(List.of(new RoomSubscriptionResponse(10L, 20L, true)));

        ToolResult<RoomUnsubscriptionToolResponse> result = tool.unsubscribeRoom(3L, null);

        assertThat(result.success()).isTrue();
        assertThat(result.data().unsubscribedRoomId()).isNull();
        assertThat(result.data().subscribedRooms())
                .containsExactly(new RoomSubResponse(20L, null, true));
        verify(coreSubscriptionRoomService, never()).unsubscribeFromRoom(1L, UserRole.NORMAL, 3L, 20L);
        verifyNoInteractions(conversationContextService);
    }

    @Test
    @DisplayName("팀 ID가 없으면 구독 취소하지 않고 팀 정보를 요청한다")
    void unsubscribeRoom_MissingTeamId() {
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.TEAM)).willReturn(null);
        given(mentionedEntityResolver.resolve(20L, MentionedEntityType.ROOM)).willReturn(20L);

        ToolResult<RoomUnsubscriptionToolResponse> result = tool.unsubscribeRoom(null, 20L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("MISSING_TEAM_ID");
        verifyNoInteractions(coreSubscriptionRoomService, conversationContextService);
    }

    @Test
    @DisplayName("구독 중인 강의실이 아니면 Core 구독 취소 API를 호출하지 않는다")
    void unsubscribeRoom_NotSubscribedRoom() {
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(21L, MentionedEntityType.ROOM)).willReturn(21L);
        given(coreSubscriptionRoomService.getSubscriptions(1L, UserRole.NORMAL, 3L))
                .willReturn(List.of(new RoomSubscriptionResponse(10L, 20L, true)));

        ToolResult<RoomUnsubscriptionToolResponse> result = tool.unsubscribeRoom(3L, 21L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("NOT_SUBSCRIBED_ROOM");
        verify(coreSubscriptionRoomService, never()).unsubscribeFromRoom(1L, UserRole.NORMAL, 3L, 21L);
        verifyNoInteractions(conversationContextService);
    }

    @Test
    @DisplayName("Core 구독 취소 API 실패 시 실패 결과를 반환한다")
    void unsubscribeRoom_CoreApiFail() {
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(20L, MentionedEntityType.ROOM)).willReturn(20L);
        given(coreSubscriptionRoomService.getSubscriptions(1L, UserRole.NORMAL, 3L))
                .willReturn(List.of(new RoomSubscriptionResponse(10L, 20L, true)));
        willThrow(new RuntimeException("core api error"))
                .given(coreSubscriptionRoomService).unsubscribeFromRoom(1L, UserRole.NORMAL, 3L, 20L);

        ToolResult<RoomUnsubscriptionToolResponse> result = tool.unsubscribeRoom(3L, 20L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("ROOM_UNSUBSCRIPTION_FAILED");
        verifyNoInteractions(conversationContextService);
    }
}
