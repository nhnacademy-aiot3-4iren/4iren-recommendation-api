package com.nhnacademy.recommendation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.building.BuildingDetailResponse;
import com.nhnacademy.recommendation.dto.llm.*;
import com.nhnacademy.recommendation.dto.room.RoomDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmConversationContextServiceTest {

    private static final String KEY = "llm:conversation-context:1";
    private static final String TELEGRAM_ROOM_KEY = "telegram:last-mentioned-room:1";
    private static final Duration TTL = Duration.ofMinutes(20);

    @Mock
    StringRedisTemplate stringRedisTemplate;

    @Mock
    ValueOperations<String, String> valueOperations;

    ObjectMapper objectMapper;
    LlmConversationContextService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        service = new LlmConversationContextService(stringRedisTemplate, objectMapper);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Redis에 값이 없으면 빈 컨텍스트를 반환한다")
    void findEmpty() {
        given(valueOperations.get(KEY)).willReturn(null);

        LlmConversationContext result = service.find(1L);

        assertThat(result).isEqualTo(LlmConversationContext.empty());
    }

    @Test
    @DisplayName("Redis에 저장된 JSON을 LlmConversationContext로 역직렬화한다")
    void find() throws Exception {
        LlmConversationContext context = new LlmConversationContext(
                "이전 질문",
                "이전 답변",
                List.of(new MentionedEntityDto(MentionedEntityType.TEAM, 3L, null)),
                LocalDateTime.of(2026, 8, 4, 10, 0)
        );
        given(valueOperations.get(KEY)).willReturn(objectMapper.writeValueAsString(context));

        LlmConversationContext result = service.find(1L);

        assertThat(result).isEqualTo(context);
    }

    @Test
    @DisplayName("Redis 값이 깨진 JSON이면 빈 컨텍스트를 반환한다")
    void findInvalidJson() {
        given(valueOperations.get(KEY)).willReturn("잘못된 JSON");

        LlmConversationContext result = service.find(1L);

        assertThat(result).isEqualTo(LlmConversationContext.empty());
    }

    @Test
    @DisplayName("save는 prefix key와 TTL을 사용해 컨텍스트 JSON을 저장한다")
    void save() throws Exception {
        LlmConversationContext context = new LlmConversationContext(
                "질문",
                "답변",
                List.of(new MentionedEntityDto(MentionedEntityType.ROOM, 20L, "201호")),
                LocalDateTime.of(2026, 8, 4, 10, 0)
        );
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        service.save(1L, context);

        verify(valueOperations).set(eq(KEY), jsonCaptor.capture(), eq(TTL));
        LlmConversationContext savedContext = objectMapper.readValue(jsonCaptor.getValue(), LlmConversationContext.class);
        assertThat(savedContext).isEqualTo(context);
    }

    @Test
    @DisplayName("saveLastExchange는 기존 컨텍스트를 유지하고 최근 질문과 답변을 저장한다")
    void saveLastExchange() throws Exception {
        LlmConversationContext context = new LlmConversationContext(
                "이전 질문",
                "이전 답변",
                List.of(new MentionedEntityDto(MentionedEntityType.TEAM, 3L, null)),
                LocalDateTime.of(2026, 8, 4, 9, 0)
        );
        given(valueOperations.get(KEY)).willReturn(objectMapper.writeValueAsString(context));
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        service.saveLastExchange(1L, "현재 질문", "현재 답변");

        verify(valueOperations).set(eq(KEY), jsonCaptor.capture(), eq(TTL));
        LlmConversationContext savedContext = objectMapper.readValue(jsonCaptor.getValue(), LlmConversationContext.class);
        assertThat(savedContext.lastQuestion()).isEqualTo("현재 질문");
        assertThat(savedContext.lastAnswer()).isEqualTo("현재 답변");
        assertThat(savedContext.mentions()).containsExactly(new MentionedEntityDto(MentionedEntityType.TEAM, 3L, null));
    }

    @Test
    @DisplayName("saveBuildingDetailMentions는 TEAM과 BUILDING 언급을 저장한다")
    void saveBuildingDetailMentions() throws Exception {
        useRedisValue(null);
        BuildingDetailResponse response = new BuildingDetailResponse(
                10L,
                3L,
                "본관",
                "본관 설명",
                null,
                null,
                null,
                0L,
                0L,
                0L
        );
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        service.saveBuildingDetailMentions(1L, 3L, response);

        verify(valueOperations, org.mockito.Mockito.times(2)).set(eq(KEY), jsonCaptor.capture(), eq(TTL));
        LlmConversationContext lastSavedContext = objectMapper.readValue(jsonCaptor.getAllValues().getLast(), LlmConversationContext.class);
        assertThat(lastSavedContext.mentions()).containsExactly(
                new MentionedEntityDto(MentionedEntityType.BUILDING, 10L, "본관"),
                new MentionedEntityDto(MentionedEntityType.TEAM, 3L, null)
        );
    }

    @Test
    @DisplayName("saveRoomDetailMentions는 TEAM, BUILDING, ROOM 언급을 저장한다")
    void saveRoomDetailMentions() throws Exception {
        useRedisValue(null);
        RoomDetailResponse response = new RoomDetailResponse(20L, 10L, "본관", "201호", "강의실", 2L, 3L);
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        service.saveRoomDetailMentions(1L, 3L, response);

        verify(valueOperations, org.mockito.Mockito.times(3)).set(eq(KEY), jsonCaptor.capture(), eq(TTL));
        LlmConversationContext lastSavedContext = objectMapper.readValue(jsonCaptor.getAllValues().getLast(), LlmConversationContext.class);
        assertThat(lastSavedContext.mentions()).containsExactly(
                new MentionedEntityDto(MentionedEntityType.ROOM, 20L, "201호"),
                new MentionedEntityDto(MentionedEntityType.BUILDING, 10L, null),
                new MentionedEntityDto(MentionedEntityType.TEAM, 3L, null)
        );
    }

    @Test
    @DisplayName("null ID mention 저장은 Redis에 쓰지 않는다")
    void saveNullMentionId() {
        service.saveTeamMention(1L, null);
        service.saveBuildingMention(1L, null, "본관");
        service.saveRoomMention(1L, null, "201호");

        verify(valueOperations, never()).set(eq(KEY), org.mockito.ArgumentMatchers.anyString(), eq(TTL));
        verifyNoMoreInteractions(valueOperations);
    }

    @Test
    @DisplayName("텔레그램 요청의 mention 저장은 recommendation에서 Redis에 쓰지 않는다")
    void telegramRequestDoesNotSaveMention() {
        LlmRequestContextHolder contextHolder = new LlmRequestContextHolder();
        LlmConversationContextService telegramService = new LlmConversationContextService(
                stringRedisTemplate,
                objectMapper,
                contextHolder
        );
        contextHolder.set(new LlmRequestContext(
                1L,
                UserRole.NORMAL,
                LlmConversationContext.empty(),
                RequestSource.TELEGRAM,
                List.of()
        ));

        try {
            telegramService.saveRoomMention(1L, 20L, "201호");

            verify(valueOperations, never()).set(eq(TELEGRAM_ROOM_KEY), eq("20"), eq(TTL));
            verify(valueOperations, never()).set(eq(KEY), org.mockito.ArgumentMatchers.anyString(), eq(TTL));
        } finally {
            contextHolder.clear();
        }
    }

    @Test
    @DisplayName("텔레그램 최근 언급 강의실 키에서 roomId를 조회한다")
    void findTelegramLastMentionedRoomId() {
        given(valueOperations.get(TELEGRAM_ROOM_KEY)).willReturn("20");

        Long result = service.findTelegramLastMentionedRoomId(1L);

        assertThat(result).isEqualTo(20L);
    }

    private void useRedisValue(String initialValue) {
        AtomicReference<String> redisValue = new AtomicReference<>(initialValue);
        given(valueOperations.get(KEY)).willAnswer(invocation -> redisValue.get());
        doAnswer(invocation -> {
            redisValue.set(invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(eq(KEY), org.mockito.ArgumentMatchers.anyString(), eq(TTL));
    }
}
