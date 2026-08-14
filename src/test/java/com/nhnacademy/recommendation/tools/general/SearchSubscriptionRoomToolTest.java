package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.llm.LlmConversationContext;
import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityType;
import com.nhnacademy.recommendation.dto.llm.RequestSource;
import com.nhnacademy.recommendation.dto.roomsub.RoomSubResponse;
import com.nhnacademy.recommendation.dto.roomsub.RoomSubscriptionResponse;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SearchSubscriptionRoomToolTest {
    @Mock
    CoreSubscriptionRoomService coreSubscriptionRoomService;

    @Mock
    LlmRequestContextHolder contextHolder;

    @Mock
    LlmConversationContextService conversationContextService;

    @Mock
    MentionedEntityResolver mentionedEntityResolver;

    SearchSubscriptionRoomTool tool;
    LlmRequestContext context;

    @BeforeEach
    void setUp() {
        context = new LlmRequestContext(1L, UserRole.NORMAL, LlmConversationContext.empty());
        tool = new SearchSubscriptionRoomTool(coreSubscriptionRoomService, contextHolder, conversationContextService, mentionedEntityResolver);
    }

    @Test
    @DisplayName("사용자가 구독한 강의실 목록 조회(TeamId)")
    void getSubscriptionRooms(){
        List<RoomSubscriptionResponse> roomSubscriptions = List.of(new RoomSubscriptionResponse(1L, 20L, true));

        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(coreSubscriptionRoomService.getSubscriptions(1L, UserRole.NORMAL, 3L)).willReturn(roomSubscriptions);

        ToolResult<List<RoomSubscriptionResponse>> result = tool.getSubscriptionRoomsByUserAndTeam(3L);

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo("SUCCESS");
        assertThat(result.data()).containsExactlyElementsOf(roomSubscriptions);
        verify(conversationContextService).saveTeamMention(1L, 3L);

    }


    @Test
    @DisplayName("텔레그램 요청에 구독 강의실 목록이 있으면 Core API를 호출하지 않고 요청 목록을 반환한다")
    void getSubscriptionRooms_TelegramProvidedRoomSubInfo() {
        LlmRequestContext telegramContext = new LlmRequestContext(
                1L,
                UserRole.NORMAL,
                LlmConversationContext.empty(),
                RequestSource.TELEGRAM,
                List.of(new RoomSubResponse(20L, "201호", true))
        );

        given(contextHolder.get()).willReturn(telegramContext);

        ToolResult<List<RoomSubscriptionResponse>> result = tool.getSubscriptionRoomsByUserAndTeam(null);

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo("SUCCESS");
        assertThat(result.data()).containsExactly(new RoomSubscriptionResponse(null, 20L, true));
        verifyNoInteractions(mentionedEntityResolver);
        verifyNoInteractions(coreSubscriptionRoomService);
        verifyNoInteractions(conversationContextService);
    }


    @Test
    @DisplayName("사용자가 구독한 강의실 목록 조회(TeamId) 실패 - 팀ID 오류")
    void getSubscriptionRooms_Fail_TeamID(){
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.TEAM)).willReturn(null);

        ToolResult<List<RoomSubscriptionResponse>> result = tool.getSubscriptionRoomsByUserAndTeam(null);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("MISSING_TEAM_ID");
        assertThat(result.data()).isNull();
        verifyNoInteractions(coreSubscriptionRoomService);
        verifyNoInteractions(conversationContextService);
    }

    @Test
    @DisplayName("사용자가 구독한 강의실 목록 조회(TeamId) 실패 - Core API 오류")
    void getSubscriptionRooms_Fail_CoreAPI(){
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(coreSubscriptionRoomService.getSubscriptions(1L, UserRole.NORMAL, 3L)).willThrow(new RuntimeException());

        ToolResult<List<RoomSubscriptionResponse>> result = tool.getSubscriptionRoomsByUserAndTeam(3L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("SUBSCRIPTION_ROOM_LIST_QUERY_FAILED");
        assertThat(result.data()).isNull();
        verifyNoInteractions(conversationContextService);

    }
}
