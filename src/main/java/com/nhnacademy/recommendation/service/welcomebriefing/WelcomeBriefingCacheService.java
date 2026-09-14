package com.nhnacademy.recommendation.service.welcomebriefing;

import com.nhnacademy.recommendation.config.CacheConfig;
import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class WelcomeBriefingCacheService {

    private final WelcomeBriefingService welcomeBriefingService;

    @Cacheable(
            cacheNames = CacheConfig.WELCOME_BRIEFING_CACHE,
            key = "#teamId + ':' + #roomId + ':' + #date",
            unless = "#result == null"
    )
    public WelcomeBriefingResponse generateWelcomeBriefing(Long teamId, Long roomId, LocalDate date) {
        return welcomeBriefingService.generateWelcomeBriefing(teamId, roomId);
    }

    @CacheEvict(cacheNames = CacheConfig.WELCOME_BRIEFING_CACHE, allEntries = true)
    public void clearWelcomeBriefingCache() {
    }
}
