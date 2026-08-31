package com.nhnacademy.recommendation.dto.dailysummary;

import com.nhnacademy.recommendation.dto.kma.KmaWeatherHistoryResponseDto;
import com.nhnacademy.recommendation.dto.room.RoomDetailResponse;
import com.nhnacademy.recommendation.dto.room.RoomRegionResponse;
import com.nhnacademy.recommendation.dto.sensor.SensorMetricSeriesResponse;

import java.time.LocalDate;

public record DailySummaryContext(
        Long teamId,
        Long roomId,
        LocalDate date,
        AnalysisPeriod analysisPeriod,
        RoomDetailResponse room,
        RoomRegionResponse region,
        SensorMetricSeriesResponse indoorSensorSeries,
        KmaWeatherHistoryResponseDto outdoorWeatherHistory
) {
    public record AnalysisPeriod(
            Integer startHour,
            Integer endHour,
            String timezone,
            String interval
    ) {
    }
}
