package com.nhnacademy.recommendation.service.core;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.dto.kma.KmaCurrentWeatherResponseDto;
import com.nhnacademy.recommendation.dto.kma.KmaForecastWeatherResponseDto;
import com.nhnacademy.recommendation.dto.kma.KmaWeatherHistoryResponseDto;
import com.nhnacademy.recommendation.exception.RequiredValueException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import static com.nhnacademy.recommendation.service.core.CoreRequestValidator.requireNonNull;
import static com.nhnacademy.recommendation.service.core.CoreRequestValidator.requireText;

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

    public KmaWeatherHistoryResponseDto getWeatherHistory(
            String regionName,
            LocalDate date,
            Integer startHour,
            Integer endHour
    ) {
        requireText(regionName, "regionName");
        requireNonNull(date, "date");
        requireHour(startHour, "startHour");
        requireHour(endHour, "endHour");
        if (startHour > endHour) {
            //TODO 커스텀 예외 수정필요
            throw new IllegalArgumentException("startHour must be less than or equal to endHour");
        }

        try {
            return coreClient.getWeatherHistory(regionName, date, startHour, endHour).getBody();
        } catch (Exception e) {
            log.warn("[CoreWeatherService] 날씨 히스토리 조회 실패. regionName={}, date={}, startHour={}, endHour={}",
                    regionName, date, startHour, endHour, e);
            throw e;
        }
    }

    private void requireHour(Integer hour, String type) {
        if (hour == null) {
            throw new RequiredValueException(type);
        }
        if (hour < 0 || hour > 23) {
            //TODO 커스텀 예외 수정필요
            throw new IllegalArgumentException("%s must be between 0 and 23. value=%s".formatted(type, hour));
        }
    }
}
