package com.nhnacademy.recommendation.service;

import com.nhnacademy.recommendation.config.CacheConfig;
import com.nhnacademy.recommendation.dto.kma.KmaWeatherHistoryResponseDto;
import com.nhnacademy.recommendation.service.core.CoreWeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DailyWeatherCacheService {

    public static final int DEFAULT_START_HOUR = 9;
    public static final int DEFAULT_END_HOUR = 18;

    private final CoreWeatherService coreWeatherService;

    @Cacheable(
            cacheNames = CacheConfig.DAILY_WEATHER_CACHE,
            key = "#regionName + ':' + #date + ':' + #startHour + '-' + #endHour",
            unless = "#result == null"
    )
    public KmaWeatherHistoryResponseDto getDailyWeather(
            String regionName,
            LocalDate date,
            Integer startHour,
            Integer endHour
    ) {
        return coreWeatherService.getWeatherHistory(regionName, date, startHour, endHour);
    }

    @Cacheable(
            cacheNames = CacheConfig.DAILY_WEATHER_CACHE,
            key = "#regionName + ':' + #date + ':9-18'",
            unless = "#result == null"
    )
    public KmaWeatherHistoryResponseDto getDailyWeather(String regionName, LocalDate date) {
        return coreWeatherService.getWeatherHistory(regionName, date, DEFAULT_START_HOUR, DEFAULT_END_HOUR);
    }
}
