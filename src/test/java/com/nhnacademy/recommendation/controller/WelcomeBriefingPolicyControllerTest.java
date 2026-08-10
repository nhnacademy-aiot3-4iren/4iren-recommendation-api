package com.nhnacademy.recommendation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.welcomeBriefing.WelcomeBriefingPolicyDto;
import com.nhnacademy.recommendation.dto.welcomeBriefing.WelcomeBriefingPolicyEnabledRequest;
import com.nhnacademy.recommendation.entity.WelcomeBriefingPolicy;
import com.nhnacademy.recommendation.exception.GlobalExceptionHandler;
import com.nhnacademy.recommendation.exception.PolicyAccessDeniedException;
import com.nhnacademy.recommendation.exception.PolicyDuplicateException;
import com.nhnacademy.recommendation.service.welcomebriefing.WelcomeBriefingPolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(WelcomeBriefingPolicyController.class)
@Import(GlobalExceptionHandler.class)
class WelcomeBriefingPolicyControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    WelcomeBriefingPolicyService service;

    @Test
    @DisplayName("GET 정책 조회 요청을 서비스에 위임한다")
    void getPolicy() throws Exception {
        given(service.getPolicyOrDefault(3L, 10L))
                .willReturn(new WelcomeBriefingPolicyDto(30, 60, 8.0, 70, true));

        mockMvc.perform(get("/api/recommendation/welcome-briefing/policies")
                        .param("teamId", "3")
                        .param("roomId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rainPossibleProbability").value(30))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @DisplayName("POST 정책 생성 요청을 서비스에 위임하고 생성 응답을 반환한다")
    void createPolicy() throws Exception {
        WelcomeBriefingPolicyDto request = new WelcomeBriefingPolicyDto(30, 60, 8.0, 70, true);
        WelcomeBriefingPolicy entity = new WelcomeBriefingPolicy(3L, 10L, 30, 60, 8.0, 70, true);
        given(service.createPolicy(eq(1L), eq(UserRole.NORMAL), eq(3L), eq(10L), any(WelcomeBriefingPolicyDto.class)))
                .willReturn(entity);

        mockMvc.perform(post("/api/recommendation/welcome-briefing/policies")
                        .header("X-USER-ID", "1")
                        .header("X-USER-ROLE", "NORMAL")
                        .param("teamId", "3")
                        .param("roomId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.teamId").value(3))
                .andExpect(jsonPath("$.roomId").value(10))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @DisplayName("PUT 정책 수정 요청을 서비스에 위임한다")
    void updatePolicy() throws Exception {
        WelcomeBriefingPolicyDto request = new WelcomeBriefingPolicyDto(25, 55, 7.0, 65, false);
        WelcomeBriefingPolicy entity = new WelcomeBriefingPolicy(3L, 10L, 25, 55, 7.0, 65, false);
        given(service.updatePolicy(eq(1L), eq(UserRole.NORMAL), eq(3L), eq(10L), any(WelcomeBriefingPolicyDto.class)))
                .willReturn(entity);

        mockMvc.perform(put("/api/recommendation/welcome-briefing/policies")
                        .header("X-USER-ID", "1")
                        .header("X-USER-ROLE", "NORMAL")
                        .param("teamId", "3")
                        .param("roomId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rainPossibleProbability").value(25))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @DisplayName("PATCH 정책 활성화 상태 변경 요청을 서비스에 위임한다")
    void updatePolicyEnabled() throws Exception {
        WelcomeBriefingPolicyEnabledRequest request = new WelcomeBriefingPolicyEnabledRequest(false);

        mockMvc.perform(patch("/api/recommendation/welcome-briefing/policies/enabled")
                        .header("X-USER-ID", "1")
                        .header("X-USER-ROLE", "NORMAL")
                        .param("teamId", "3")
                        .param("roomId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(service).updatePolicyEnabled(1L, UserRole.NORMAL, 3L, 10L, false);
    }

    @Test
    @DisplayName("DELETE 정책 삭제 요청을 서비스에 위임한다")
    void deletePolicy() throws Exception {
        mockMvc.perform(delete("/api/recommendation/welcome-briefing/policies")
                        .header("X-USER-ID", "1")
                        .header("X-USER-ROLE", "NORMAL")
                        .param("teamId", "3")
                        .param("roomId", "10"))
                .andExpect(status().isNoContent());

        verify(service).deletePolicy(1L, UserRole.NORMAL, 3L, 10L);
    }

    @Test
    @DisplayName("정책 중복 예외는 409 응답을 반환한다")
    void duplicatePolicy() throws Exception {
        WelcomeBriefingPolicyDto request = new WelcomeBriefingPolicyDto(30, 60, 8.0, 70, true);
        given(service.createPolicy(eq(1L), eq(UserRole.NORMAL), eq(3L), eq(10L), any(WelcomeBriefingPolicyDto.class)))
                .willThrow(new PolicyDuplicateException(3L, 10L));

        mockMvc.perform(post("/api/recommendation/welcome-briefing/policies")
                        .header("X-USER-ID", "1")
                        .header("X-USER-ROLE", "NORMAL")
                        .param("teamId", "3")
                        .param("roomId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 존재하는 정책입니다. teamId:3, roomId:10"));
    }

    @Test
    @DisplayName("정책 권한 예외는 403 응답을 반환한다")
    void accessDenied() throws Exception {
        WelcomeBriefingPolicyDto request = new WelcomeBriefingPolicyDto(30, 60, 8.0, 70, true);
        given(service.createPolicy(eq(1L), eq(UserRole.NORMAL), eq(3L), eq(10L), any(WelcomeBriefingPolicyDto.class)))
                .willThrow(new PolicyAccessDeniedException(1L, 3L));

        mockMvc.perform(post("/api/recommendation/welcome-briefing/policies")
                        .header("X-USER-ID", "1")
                        .header("X-USER-ROLE", "NORMAL")
                        .param("teamId", "3")
                        .param("roomId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("정책을 관리할 권한이 없습니다. userId:1, teamId:3"));
    }
}
