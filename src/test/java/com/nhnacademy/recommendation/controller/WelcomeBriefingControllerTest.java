package com.nhnacademy.recommendation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingRequest;
import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingResponse;
import com.nhnacademy.recommendation.exception.GlobalExceptionHandler;
import com.nhnacademy.recommendation.exception.RequiredValueException;
import com.nhnacademy.recommendation.exception.RoomPreferenceNotFoundException;
import com.nhnacademy.recommendation.service.welcomebriefing.WelcomeBriefingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(WelcomeBriefingController.class)
@Import(GlobalExceptionHandler.class)
class WelcomeBriefingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    WelcomeBriefingService welcomeBriefingService;

    @Test
    @DisplayName("웰컴 브리핑 생성 요청을 서비스에 위임한다")
    void generateWelcomeBriefing() throws Exception {
        WelcomeBriefingRequest request = new WelcomeBriefingRequest(3L, 10L);
        WelcomeBriefingResponse response = new WelcomeBriefingResponse(
                "오늘은 환기 관리가 필요합니다.",
                "CO2가 빠르게 상승 중입니다.",
                "비와 강풍 가능성이 있어 창문 개방은 주의가 필요합니다.",
                List.of("환기장치를 점검하세요."),
                List.of("센서 수신 상태를 확인하세요.")
        );

        given(welcomeBriefingService.generateWelcomeBriefing(3L, 10L)).willReturn(response);

        mockMvc.perform(post("/api/recommendation/welcome-briefing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("오늘은 환기 관리가 필요합니다."))
                .andExpect(jsonPath("$.currentStatus").value("CO2가 빠르게 상승 중입니다."))
                .andExpect(jsonPath("$.recommendations[0]").value("환기장치를 점검하세요."));

        verify(welcomeBriefingService).generateWelcomeBriefing(3L, 10L);
    }

    @Test
    @DisplayName("필수 값이 없으면 400 응답을 반환한다")
    void generateWelcomeBriefing_RequiredValue() throws Exception {
        WelcomeBriefingRequest request = new WelcomeBriefingRequest(null, 10L);

        given(welcomeBriefingService.generateWelcomeBriefing(null, 10L))
                .willThrow(new RequiredValueException("teamId"));

        mockMvc.perform(post("/api/recommendation/welcome-briefing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("teamId는 필수 값입니다."));
    }

    @Test
    @DisplayName("모델 Bundle에 강의실 선호 정보가 없으면 404 응답을 반환한다")
    void generateWelcomeBriefing_RoomPreferenceNotFound() throws Exception {
        WelcomeBriefingRequest request = new WelcomeBriefingRequest(3L, 10L);
        RoomPreferenceNotFoundException exception = new RoomPreferenceNotFoundException(10L);

        given(welcomeBriefingService.generateWelcomeBriefing(3L, 10L)).willThrow(exception);

        mockMvc.perform(post("/api/recommendation/welcome-briefing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(exception.getMessage()))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("요청 body가 없으면 400 응답을 반환한다")
    void generateWelcomeBriefing_EmptyBody() throws Exception {
        mockMvc.perform(post("/api/recommendation/welcome-briefing")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("WelcomeBriefingRequest는 필수 값입니다."));
    }
}
