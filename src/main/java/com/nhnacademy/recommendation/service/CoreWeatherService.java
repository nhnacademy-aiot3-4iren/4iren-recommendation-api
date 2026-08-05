package com.nhnacademy.recommendation.service;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.dto.kma.KmaCurrentWeatherResponseDto;
import com.nhnacademy.recommendation.dto.kma.KmaForecastWeatherResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.nhnacademy.recommendation.service.CoreRequestValidator.requireText;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoreWeatherService {

    private final CoreClient coreClient;

    public KmaCurrentWeatherResponseDto getCurrentWeather(String regionName) {
        requireText(regionName, "regionName");

        try {
            return coreClient.getNcst(regionName).getBody();
        } catch (Exception e) {
            log.warn("[CoreWeatherService] 현재 날씨 조회 실패. regionName={}", regionName, e);
            throw e;
        }
    }

    public KmaForecastWeatherResponseDto getForecastWeather(String regionName) {
        requireText(regionName, "regionName");

        try {
            return coreClient.getFcst(regionName).getBody();
        } catch (Exception e) {
            log.warn("[CoreWeatherService] 날씨 예보 조회 실패. regionName={}", regionName, e);
            throw e;
        }
    }
}
