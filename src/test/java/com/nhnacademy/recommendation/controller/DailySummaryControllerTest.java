package com.nhnacademy.recommendation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.dto.dailysummary.DailySummaryRequest;
import com.nhnacademy.recommendation.dto.dailysummary.DailySummaryResponse;
import com.nhnacademy.recommendation.exception.GlobalExceptionHandler;
import com.nhnacademy.recommendation.service.dailysummary.DailySummaryCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(DailySummaryController.class)
@Import(GlobalExceptionHandler.class)
class DailySummaryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    DailySummaryCacheService dailySummaryCacheService;

    @Test
    @DisplayName("하루 요약 생성 요청을 서비스에 위임한다")
    void generateDailySummary() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 20);
        DailySummaryRequest request = new DailySummaryRequest(3L, 10L, date, null, null);
        DailySummaryResponse response = new DailySummaryResponse(
                "하루 동안 실내 온도가 상승했습니다.",
                "실내 온도 상승이 확인됩니다.",
                "외부 온도도 높았습니다.",
                "외부가 더워 환기 판단이 필요합니다.",
                List.of("CO2를 확인하세요."),
                List.of("일부 날씨 데이터가 누락되었습니다.")
        );

        given(dailySummaryCacheService.generateDailySummary(3L, 10L, date, 9, 18))
                .willReturn(response);

        mockMvc.perform(post("/api/recommendation/daily-summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("하루 동안 실내 온도가 상승했습니다."))
                .andExpect(jsonPath("$.indoorEnvironment").value("실내 온도 상승이 확인됩니다."))
                .andExpect(jsonPath("$.recommendations[0]").value("CO2를 확인하세요."));

        verify(dailySummaryCacheService).generateDailySummary(3L, 10L, date, 9, 18);
    }

    @Test
    @DisplayName("요청한 분석 시간대가 있으면 그대로 서비스에 전달한다")
    void generateDailySummary_CustomHours() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 20);
        DailySummaryRequest request = new DailySummaryRequest(3L, 10L, date, 10, 17);

        given(dailySummaryCacheService.generateDailySummary(3L, 10L, date, 10, 17))
                .willReturn(new DailySummaryResponse("요약", "실내", "외부", "비교", List.of(), List.of()));

        mockMvc.perform(post("/api/recommendation/daily-summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(dailySummaryCacheService).generateDailySummary(3L, 10L, date, 10, 17);
    }

    @Test
    @DisplayName("요청 body가 없으면 400 응답을 반환한다")
    void generateDailySummary_EmptyBody() throws Exception {
        mockMvc.perform(post("/api/recommendation/daily-summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("DailySummaryRequest는 필수 값입니다."));
    }

    @Test
    @DisplayName("하루 요약 캐시를 삭제한다")
    void clearDailySummaryCache() throws Exception {
        mockMvc.perform(delete("/api/recommendation/daily-summary/cache"))
                .andExpect(status().isNoContent());

        verify(dailySummaryCacheService).clearDailySummaryCache();
    }
}
