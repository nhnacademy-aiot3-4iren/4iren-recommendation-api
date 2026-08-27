package com.nhnacademy.recommendation.service.dailysummary;

import com.nhnacademy.recommendation.config.CacheConfig;
import com.nhnacademy.recommendation.dto.dailysummary.DailySummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DailySummaryCacheService {

    private final DailySummaryService dailySummaryService;

    @Cacheable(
            cacheNames = CacheConfig.DAILY_SUMMARY_CACHE,
            key = "#teamId + ':' + #roomId + ':' + #date + ':' + #startHour + '-' + #endHour",
            unless = "#result == null"
    )
    public DailySummaryResponse generateDailySummary(Long teamId,
                                                     Long roomId,
                                                     LocalDate date,
                                                     Integer startHour,
                                                     Integer endHour) {
        return dailySummaryService.generateDailySummary(teamId, roomId, date, startHour, endHour);
    }

    @CacheEvict(cacheNames = CacheConfig.DAILY_SUMMARY_CACHE, allEntries = true)
    public void clearDailySummaryCache() {
    }
}
