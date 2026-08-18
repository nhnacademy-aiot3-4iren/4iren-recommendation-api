package com.nhnacademy.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.building.BuildingDetailResponse;
import com.nhnacademy.recommendation.dto.building.BuildingResponse;
import com.nhnacademy.recommendation.dto.llm.*;
import com.nhnacademy.recommendation.dto.room.RoomDetailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class LlmConversationContextService {

    private static final Duration TTL = Duration.ofMinutes(20);
    private static final String PREFIX = "llm:conversation-context:";
    private static final String TELEGRAM_LAST_MENTIONED_ROOM_PREFIX = "telegram:last-mentioned-room:";
    private static final String TELEGRAM_CHAT_MEMORY_CONTEXT_PREFIX = "telegram:chat-memory:context:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final LlmRequestContextHolder llmRequestContextHolder;

    public LlmConversationContextService(StringRedisTemplate stringRedisTemplate,
                                         ObjectMapper objectMapper) {
        this(stringRedisTemplate, objectMapper, null);
    }

    @Autowired
    public LlmConversationContextService(StringRedisTemplate stringRedisTemplate,
                                         ObjectMapper objectMapper,
                                         LlmRequestContextHolder llmRequestContextHolder) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.llmRequestContextHolder = llmRequestContextHolder;
    }

    public LlmConversationContext find(Long userId) {
        String value = stringRedisTemplate.opsForValue().get(key(userId));
        if (value == null || value.isBlank()) {
            return LlmConversationContext.empty();
        }

        try {
            return objectMapper.readValue(value, LlmConversationContext.class);
        } catch (JsonProcessingException e) {
            log.warn("LLM 대화 컨텍스트 역직렬화 실패. userId={}", userId, e);
            return LlmConversationContext.empty();
        }
    }

    public void saveLastExchange(Long userId, String question, String answer) {
        if (isTelegramRequest()) {
            return;
        }
        save(userId, find(userId).withLastExchange(question, answer));
    }

    public void saveMention(Long userId, MentionedEntityDto mention) {
        if (isTelegramRequest()) {
            return;
        }
        save(userId, find(userId).withMention(mention));
    }

    public void saveTeamMention(Long userId, Long teamId) {
        if (teamId != null) {
            saveMention(userId, new MentionedEntityDto(MentionedEntityType.TEAM, teamId, null));
        }
    }

    public void saveBuildingMention(Long userId, Long buildingId, String buildingName) {
        if (buildingId != null) {
            saveMention(userId, new MentionedEntityDto(MentionedEntityType.BUILDING, buildingId, buildingName));
        }
    }

    public void saveRoomMention(Long userId, Long roomId, String roomName) {
        if (roomId != null) {
            saveMention(userId, new MentionedEntityDto(MentionedEntityType.ROOM, roomId, roomName));
        }
    }

    public void saveBuildingListMentions(Long userId, Long teamId) {
        saveTeamMention(userId, teamId);
    }

    public void saveBuildingDetailMentions(Long userId, Long teamId, BuildingDetailResponse response) {
        saveTeamMention(userId, teamId);
        if (response != null) {
            saveBuildingMention(userId, response.buildingId(), response.buildingName());
        }
    }

    public void saveBuildingSummaryMentions(Long userId, Long teamId, BuildingResponse response) {
        saveTeamMention(userId, teamId);
        if (response != null) {
            saveBuildingMention(userId, response.buildingId(), response.buildingName());
        }
    }

    public void saveRoomListMentions(Long userId, Long teamId, Long buildingId) {
        saveTeamMention(userId, teamId);
        saveBuildingMention(userId, buildingId, null);
    }

    public void saveRoomDetailMentions(Long userId, Long teamId, RoomDetailResponse response) {
        saveTeamMention(userId, teamId);
        if (response != null) {
            saveBuildingMention(userId, response.buildingId(), null);
            saveRoomMention(userId, response.roomId(), response.roomName());
        }
    }

    public void save(Long userId, LlmConversationContext context) {
        if (isTelegramRequest()) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(key(userId), objectMapper.writeValueAsString(context), TTL);
        } catch (JsonProcessingException e) {
            log.warn("LLM 대화 컨텍스트 직렬화 실패. userId={}", userId, e);
        }
    }

    public Long findTelegramLastMentionedRoomId(Long userId) {
        String value = stringRedisTemplate.opsForValue().get(telegramLastMentionedRoomKey(userId));
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            log.warn("텔레그램 최근 언급 강의실 ID 파싱 실패. userId={}, value={}", userId, value, e);
            return null;
        }
    }

    public LlmConversationContext findTelegramConversationContext(Long userId) {
        String value = stringRedisTemplate.opsForValue().get(telegramChatMemoryContextKey(userId));
        if (value == null || value.isBlank()) {
            return LlmConversationContext.empty();
        }

        try {
            TelegramConversationContext context = objectMapper.readValue(value, TelegramConversationContext.class);
            if (context == null || !context.isQuestion()) {
                return LlmConversationContext.empty();
            }
            return LlmConversationContext.empty()
                    .withLastExchange(context.lastQuestion(), context.lastAnswer());
        } catch (JsonProcessingException e) {
            log.warn("텔레그램 대화 컨텍스트 역직렬화 실패. userId={}", userId, e);
            return LlmConversationContext.empty();
        }
    }

    private boolean isTelegramRequest() {
        if (llmRequestContextHolder == null) {
            return false;
        }
        try {
            LlmRequestContext context = llmRequestContextHolder.get();
            return context.source() == RequestSource.TELEGRAM;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String key(Long userId) {
        return PREFIX + userId;
    }

    private String telegramLastMentionedRoomKey(Long userId) {
        return TELEGRAM_LAST_MENTIONED_ROOM_PREFIX + userId;
    }

    private String telegramChatMemoryContextKey(Long userId) {
        return TELEGRAM_CHAT_MEMORY_CONTEXT_PREFIX + userId;
    }
}
