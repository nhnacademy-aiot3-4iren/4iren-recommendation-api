package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.llm.LlmConversationContext;
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
class SubscribeRoomToolTest {

    @Mock
    CoreSubscriptionRoomService coreSubscriptionRoomService;

    @Mock
    CoreRoomService coreRoomService;

    @Mock
    LlmRequestContextHolder contextHolder;

    @Mock
    LlmConversationContextService conversationContextService;

    @Mock
    MentionedEntityResolver mentionedEntityResolver;

    SubscribeRoomTool tool;
    LlmRequestContext context;

    @BeforeEach
    void setUp() {
        context = new LlmRequestContext(1L, UserRole.NORMAL, LlmConversationContext.empty());
        tool = new SubscribeRoomTool(
                coreSubscriptionRoomService,
                coreRoomService,
                contextHolder,
                conversationContextService,
                mentionedEntityResolver
        );
    }

    @Test
    @DisplayName("강의실 ID가 있으면 중복 확인 후 강의실을 구독한다")
    void subscribeRoom() {
        RoomSubscriptionResponse subscription = new RoomSubscriptionResponse(10L, 20L, true);

        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.BUILDING)).willReturn(null);
        given(mentionedEntityResolver.resolve(20L, MentionedEntityType.ROOM)).willReturn(20L);
        given(coreSubscriptionRoomService.getSubscriptions(1L, UserRole.NORMAL, 3L)).willReturn(List.of());
        given(coreSubscriptionRoomService.subscribeToRoom(1L, UserRole.NORMAL, 3L, 20L)).willReturn(subscription);

        ToolResult<RoomSubscriptionToolResponse> result = tool.subscribeRoom(3L, null, 20L);

        assertThat(result.success()).isTrue();
        assertThat(result.data().subscription()).isEqualTo(subscription);
        assertThat(result.data().availableRooms()).isEmpty();
        verify(conversationContextService).saveTeamMention(1L, 3L);
        verify(conversationContextService).saveRoomMention(1L, 20L, null);
    }

    @Test
    @DisplayName("요청 컨텍스트 구독 목록에 이미 있으면 Core 구독 API를 호출하지 않는다")
    void subscribeRoom_AlreadySubscribedFromContext() {
        LlmRequestContext telegramContext = new LlmRequestContext(
                1L,
                UserRole.NORMAL,
                LlmConversationContext.empty(),
                null,
                List.of(new RoomSubResponse(20L, "201호", true))
        );

        given(contextHolder.get()).willReturn(telegramContext);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.BUILDING)).willReturn(null);
        given(mentionedEntityResolver.resolve(20L, MentionedEntityType.ROOM)).willReturn(20L);

        ToolResult<RoomSubscriptionToolResponse> result = tool.subscribeRoom(3L, null, 20L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("ALREADY_SUBSCRIBED_ROOM");
        verifyNoInteractions(coreSubscriptionRoomService);
        verifyNoInteractions(conversationContextService);
    }

    @Test
    @DisplayName("강의실 ID가 없으면 건물 내 강의실에서 이미 구독한 강의실을 제외한 후보를 반환한다")
    void subscribeRoom_ReturnAvailableRoomCandidates() {
        List<RoomResponse> rooms = List.of(
                new RoomResponse(20L, 10L, "201호", "강의실"),
                new RoomResponse(21L, 10L, "202호", "강의실")
        );
        LlmRequestContext contextWithSubscriptions = new LlmRequestContext(
                1L,
                UserRole.NORMAL,
                LlmConversationContext.empty(),
                null,
                List.of(new RoomSubResponse(20L, "201호", true))
        );

        given(contextHolder.get()).willReturn(contextWithSubscriptions);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(10L, MentionedEntityType.BUILDING)).willReturn(10L);
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.ROOM)).willReturn(null);
        given(coreRoomService.getRoomListByBuilding(1L, UserRole.NORMAL, 3L, 10L)).willReturn(rooms);

        ToolResult<RoomSubscriptionToolResponse> result = tool.subscribeRoom(3L, 10L, null);

        assertThat(result.success()).isTrue();
        assertThat(result.data().subscription()).isNull();
        assertThat(result.data().availableRooms()).containsExactly(new RoomResponse(21L, 10L, "202호", "강의실"));
        verifyNoInteractions(coreSubscriptionRoomService);
        verify(conversationContextService).saveTeamMention(1L, 3L);
        verify(conversationContextService).saveBuildingMention(1L, 10L, null);
    }

    @Test
    @DisplayName("팀 ID가 없으면 구독하지 않고 팀 정보를 요청한다")
    void subscribeRoom_MissingTeamId() {
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.TEAM)).willReturn(null);
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.BUILDING)).willReturn(null);
        given(mentionedEntityResolver.resolve(20L, MentionedEntityType.ROOM)).willReturn(20L);

        ToolResult<RoomSubscriptionToolResponse> result = tool.subscribeRoom(null, null, 20L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("MISSING_TEAM_ID");
        verifyNoInteractions(coreSubscriptionRoomService, coreRoomService, conversationContextService);
    }

    @Test
    @DisplayName("강의실 ID도 건물 ID도 없으면 후보를 만들 수 없어 실패한다")
    void subscribeRoom_MissingRoomAndBuildingId() {
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.BUILDING)).willReturn(null);
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.ROOM)).willReturn(null);

        ToolResult<RoomSubscriptionToolResponse> result = tool.subscribeRoom(3L, null, null);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("MISSING_ROOM_SUBSCRIPTION_TARGET");
        verifyNoInteractions(coreSubscriptionRoomService, coreRoomService, conversationContextService);
    }

    @Test
    @DisplayName("양수가 아닌 강의실 ID는 Core API를 호출하지 않는다")
    void subscribeRoom_InvalidRoomId() {
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.BUILDING)).willReturn(null);
        given(mentionedEntityResolver.resolve(-1L, MentionedEntityType.ROOM)).willReturn(-1L);

        ToolResult<RoomSubscriptionToolResponse> result = tool.subscribeRoom(3L, null, -1L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("INVALID_ROOM_ID");
        verifyNoInteractions(coreSubscriptionRoomService, coreRoomService, conversationContextService);
    }

    @Test
    @DisplayName("Core 구독 API 실패 시 실패 결과를 반환한다")
    void subscribeRoom_CoreApiFail() {
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.BUILDING)).willReturn(null);
        given(mentionedEntityResolver.resolve(20L, MentionedEntityType.ROOM)).willReturn(20L);
        given(coreSubscriptionRoomService.getSubscriptions(1L, UserRole.NORMAL, 3L)).willReturn(List.of());
        given(coreSubscriptionRoomService.subscribeToRoom(1L, UserRole.NORMAL, 3L, 20L))
                .willThrow(new RuntimeException("core api error"));

        ToolResult<RoomSubscriptionToolResponse> result = tool.subscribeRoom(3L, null, 20L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("ROOM_SUBSCRIPTION_FAILED");
        verifyNoInteractions(conversationContextService);
    }
}
