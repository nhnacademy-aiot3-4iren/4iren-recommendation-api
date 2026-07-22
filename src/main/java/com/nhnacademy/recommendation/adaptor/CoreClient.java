package com.nhnacademy.recommendation.adaptor;

import com.nhnacademy.recommendation.dto.kma.KmaCurrentWeatherResponseDto;
import com.nhnacademy.recommendation.dto.kma.KmaForecastWeatherResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "4iren-core")
public interface CoreClient {

    @GetMapping("/api/kma/ultraSrtNcst")
    ResponseEntity<KmaCurrentWeatherResponseDto> getNcst(@RequestParam String regionName);

    @GetMapping("/api/kma/ultraSrtFcst")
    ResponseEntity<KmaForecastWeatherResponseDto> getFcst(@RequestParam String regionName);
}
