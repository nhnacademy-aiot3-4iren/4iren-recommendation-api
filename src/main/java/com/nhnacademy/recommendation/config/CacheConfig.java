package com.nhnacademy.recommendation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.dto.dailysummary.DailySummaryResponse;
import com.nhnacademy.recommendation.dto.kma.KmaWeatherHistoryResponseDto;
import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingResponse;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String DAILY_WEATHER_CACHE = "daily-weather";
    public static final String WELCOME_BRIEFING_CACHE = "welcome-briefing";
    public static final String DAILY_SUMMARY_CACHE = "daily-summary";

    private static final Duration DEFAULT_TTL = Duration.ofHours(1);
    private static final Duration DAILY_WEATHER_TTL = Duration.ofHours(6);
    private static final Duration WELCOME_BRIEFING_TTL = Duration.ofHours(1);
    private static final Duration DAILY_SUMMARY_TTL = Duration.ofHours(12);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisCacheConfiguration defaultConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(objectMapper)
                ));

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                DAILY_WEATHER_CACHE, defaultConfiguration
                        .entryTtl(DAILY_WEATHER_TTL)
                        .serializeValuesWith(valueSerializer(new Jackson2JsonRedisSerializer<>(
                                objectMapper,
                                KmaWeatherHistoryResponseDto.class
                        ))),
                WELCOME_BRIEFING_CACHE, defaultConfiguration
                        .entryTtl(WELCOME_BRIEFING_TTL)
                        .serializeValuesWith(valueSerializer(new Jackson2JsonRedisSerializer<>(
                                objectMapper,
                                WelcomeBriefingResponse.class
                        ))),
                DAILY_SUMMARY_CACHE, defaultConfiguration
                        .entryTtl(DAILY_SUMMARY_TTL)
                        .serializeValuesWith(valueSerializer(new Jackson2JsonRedisSerializer<>(
                                objectMapper,
                                DailySummaryResponse.class
                        )))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfiguration)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    @SuppressWarnings("unchecked")
    private RedisSerializationContext.SerializationPair<Object> valueSerializer(RedisSerializer<?> serializer) {
        return RedisSerializationContext.SerializationPair.fromSerializer((RedisSerializer<Object>) serializer);
    }
}
