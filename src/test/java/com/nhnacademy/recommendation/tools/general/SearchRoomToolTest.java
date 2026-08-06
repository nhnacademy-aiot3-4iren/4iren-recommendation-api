package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.llm.LlmConversationContext;
import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityType;
import com.nhnacademy.recommendation.dto.room.RoomDetailResponse;
import com.nhnacademy.recommendation.dto.room.RoomResponse;
import com.nhnacademy.recommendation.dto.tool.ToolResult;
import com.nhnacademy.recommendation.service.core.CoreRoomService;
import com.nhnacademy.recommendation.service.LlmConversationContextService;
import com.nhnacademy.recommendation.service.MentionedEntityResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchRoomToolTest {
    @Mock
    CoreRoomService coreRoomService;

    @Mock
    LlmRequestContextHolder contextHolder;

    @Mock
    LlmConversationContextService conversationContextService;

    @Mock
    MentionedEntityResolver mentionedEntityResolver;

    SearchRoomTool tool;
    LlmRequestContext context;

    @BeforeEach
    void setUp() {
        context = new LlmRequestContext(1L, UserRole.NORMAL, LlmConversationContext.empty());
        tool = new SearchRoomTool(coreRoomService, contextHolder, conversationContextService, mentionedEntityResolver);
    }

    @Test
    @DisplayName("강의실 목록 조회 성공")
    void getRoomList() {
        List<RoomResponse> rooms = List.of(new RoomResponse(1L, 10L, "테스트방", "테스트방 설명"));

        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(10L, MentionedEntityType.BUILDING)).willReturn(10L);

        given(coreRoomService.getRoomListByBuilding(1L, UserRole.NORMAL, 3L, 10L)).willReturn(rooms);

        ToolResult<List<RoomResponse>> result = tool.getRoomListByBuilding(3L, 10L);

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo("SUCCESS");
        assertThat(result.data()).containsExactlyElementsOf(rooms);
        verify(conversationContextService).saveRoomListMentions(1L, 3L, 10L);

    }

    @Test
    @DisplayName("강의실 목록 조회 실패 - 팀ID 오류")
    void getRoomList_Fail_TeamID(){
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.TEAM)).willReturn(null);
        given(mentionedEntityResolver.resolve(10L, MentionedEntityType.BUILDING)).willReturn(10L);


        ToolResult<List<RoomResponse>> result = tool.getRoomListByBuilding(null, 10L);


        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("MISSING_ROOM_LIST_CONDITION");
        assertThat(result.data()).isNull();
        verifyNoInteractions(coreRoomService);
        verifyNoInteractions(conversationContextService);

    }

    @Test
    @DisplayName("강의실 목록 조회 실패 - 건물ID 오류")
    void getRoomList_Fail_BuildingID(){
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.BUILDING)).willReturn(null);


        ToolResult<List<RoomResponse>> result = tool.getRoomListByBuilding(3L, null);


        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("MISSING_ROOM_LIST_CONDITION");
        assertThat(result.data()).isNull();
        verifyNoInteractions(coreRoomService);
        verifyNoInteractions(conversationContextService);

    }

    @Test
    @DisplayName("강의실 목록 조회 실패 - Core API 오류")
    void getRoomList_Fail_CoreAPI(){
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(10L, MentionedEntityType.BUILDING)).willReturn(10L);

        when(coreRoomService.getRoomListByBuilding(1L, UserRole.NORMAL, 3L, 10L)).thenThrow(new RuntimeException());


        ToolResult<List<RoomResponse>> result = tool.getRoomListByBuilding(3L, 10L);


        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("ROOM_LIST_QUERY_FAILED");
        assertThat(result.data()).isNull();
        verifyNoInteractions(conversationContextService);

    }

    @Test
    @DisplayName("강의실 세부정보 조회 성공")
    void getRoomDetail(){
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(20L, MentionedEntityType.ROOM)).willReturn(20L);

        RoomDetailResponse response = new RoomDetailResponse(20L, 10L, "테스트 건물 이름", "테스트 방 이름", "테스트 방 설명", 0L, 0L);

        given(coreRoomService.getRoomDetail(1L, UserRole.NORMAL, 3L, 20L)).willReturn(response);

        ToolResult<RoomDetailResponse> result = tool.getRoomDetail(3L, 20L);

        assertThat(result.success()).isTrue();
        assertThat(result.code()).isEqualTo("SUCCESS");
        assertThat(result.data()).isEqualTo(response);

        verify(conversationContextService).saveRoomDetailMentions(1L, 3L, response);

    }

    @Test
    @DisplayName("강의실 세부정보 조회 실패 - 팀ID 오류")
    void getRoomDetail_Fail_TeamID(){
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.TEAM)).willReturn(null);
        given(mentionedEntityResolver.resolve(20L, MentionedEntityType.ROOM)).willReturn(20L);

        ToolResult<RoomDetailResponse> result = tool.getRoomDetail(null, 20L);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("MISSING_ROOM_DETAIL_CONDITION");
        assertThat(result.data()).isNull();

        verifyNoInteractions(coreRoomService);
        verifyNoInteractions(conversationContextService);

    }

    @Test
    @DisplayName("강의실 세부정보 조회 실패 - 강의실ID 오류")
    void getRoomDetail_Fail_RoomID(){
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(null, MentionedEntityType.ROOM)).willReturn(null);

        ToolResult<RoomDetailResponse> result = tool.getRoomDetail(3L, null);

        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("MISSING_ROOM_DETAIL_CONDITION");
        assertThat(result.data()).isNull();

        verifyNoInteractions(coreRoomService);
        verifyNoInteractions(conversationContextService);

    }

    @Test
    @DisplayName("강의실 세부정보 조회 실패 - Core API 오류")
    void getRoomDetail_Fail_CoreAPI(){
        given(contextHolder.get()).willReturn(context);
        given(mentionedEntityResolver.resolve(3L, MentionedEntityType.TEAM)).willReturn(3L);
        given(mentionedEntityResolver.resolve(20L, MentionedEntityType.ROOM)).willReturn(20L);

        when(coreRoomService.getRoomDetail(1L, UserRole.NORMAL, 3L, 20L)).thenThrow(new RuntimeException());


        ToolResult<RoomDetailResponse> result = tool.getRoomDetail(3L, 20L);


        assertThat(result.success()).isFalse();
        assertThat(result.code()).isEqualTo("ROOM_DETAIL_QUERY_FAILED");
        assertThat(result.data()).isNull();
        verifyNoInteractions(conversationContextService);

    }

}
