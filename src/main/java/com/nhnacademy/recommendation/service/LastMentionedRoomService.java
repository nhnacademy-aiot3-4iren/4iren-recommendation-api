package com.nhnacademy.recommendation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 사용자별 마지막으로 언급된 방이름을 캐싱합니다.
 */
@Service
@RequiredArgsConstructor
public class LastMentionedRoomService {

    private final StringRedisTemplate stringRedisTemplate;
    private static final  Duration TTL = Duration.ofMinutes(20);
    private static final String PREFIX = "telegram:last-mentioned-room:";

    /**
     * 마지막으로 언급된 방 아이디 저장
     * 해당 키 존재 -> 덮어씌워짐
     * 해당 키 존재X -> 새로 생성
     *
     * @param userId 채팅 유저
     * @param roomId 마지막으로 언급된 방 아이디
     */
    public void save(Long userId, Long roomId){
        stringRedisTemplate.opsForValue().set(key(userId), roomId.toString(), TTL);
    }

    /**
     * 마지막으로 언급된 방 조회
     * @param userId 채팅 유저
     * @return 마지막으로 언급된 방 조회값
     */
    public Optional<Long> find(Long userId){
        String value = stringRedisTemplate.opsForValue().get(key(userId));
        return value == null ? Optional.empty() : Optional.of(Long.parseLong(value));
    }

    /**
     * redis key prefix
     * @param userId 채팅 유저
     * @return redis key
     */
    private String key(Long userId){
        return PREFIX + userId;
    }
}