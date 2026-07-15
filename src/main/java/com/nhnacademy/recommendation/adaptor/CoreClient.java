package com.nhnacademy.recommendation.adaptor;

import com.nhnacademy.recommendation.dto.kma.KmaCurrentWeatherResponseDto;
import com.nhnacademy.recommendation.dto.kma.KmaForecastWeatherResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "core-service")
public interface CoreClient {

    @GetMapping("/ultraSrtNcst")
    ResponseEntity<KmaCurrentWeatherResponseDto> getNcst(@RequestParam String regionName);

    @GetMapping("/ultraSrtFcst")
    ResponseEntity<KmaForecastWeatherResponseDto> getFcst(@RequestParam String regionName);
}
