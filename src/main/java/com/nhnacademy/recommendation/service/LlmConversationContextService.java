package com.nhnacademy.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.dto.building.BuildingDetailResponse;
import com.nhnacademy.recommendation.dto.building.BuildingResponse;
import com.nhnacademy.recommendation.dto.llm.LlmConversationContext;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityDto;
import com.nhnacademy.recommendation.dto.llm.MentionedEntityType;
import com.nhnacademy.recommendation.dto.room.RoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmConversationContextService {

    private static final Duration TTL = Duration.ofMinutes(20);
    private static final String PREFIX = "llm:conversation-context:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

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
        save(userId, find(userId).withLastExchange(question, answer));
    }

    public void saveMention(Long userId, MentionedEntityDto mention) {
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

    public void saveRoomDetailMentions(Long userId, Long teamId, RoomResponse response) {
        saveTeamMention(userId, teamId);
        if (response != null) {
            saveBuildingMention(userId, response.buildingId(), null);
            saveRoomMention(userId, response.roomId(), response.roomName());
        }
    }

    public void save(Long userId, LlmConversationContext context) {
        try {
            stringRedisTemplate.opsForValue().set(key(userId), objectMapper.writeValueAsString(context), TTL);
        } catch (JsonProcessingException e) {
            log.warn("LLM 대화 컨텍스트 직렬화 실패. userId={}", userId, e);
        }
    }

    private String key(Long userId) {
        return PREFIX + userId;
    }
}
